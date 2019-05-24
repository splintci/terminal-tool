<?php
/**
 * Splint
 *
 * An open source package and dependency manager for Code Igniter (an open
 * source application development framework for PHP).
 *
 * This content is released under the MIT License (MIT)
 *
 * Copyright (c) 2014 - 2019, British Columbia Institute of Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @package	Splint
 * @author  Splint Dev Team
 * @license	https://opensource.org/licenses/MIT	MIT License
 * @link	https://splint.cynobit.com
 * @since	Version 0.0.1
*/
if (!defined('BASEPATH')) exit('No direct script access allowed');

/**
 * [MY_Loader description]
 */
class MY_Loader extends CI_Loader {

  /**
   * splint  The funcnction that loads resources from a splint package.
   *
   * @param  string $splint   Package name (Splint or Identifier) of thee
   *                          package to load resources from.
   * @param  mixed  $autoload An assocative array of resources to load or a
   *                          single string of the resource to load with the
   *                          first character indicating the kind of resource to
   *                          load. See http://splint.cynobit.com/wiki/load_splint
   * @param  mixed   $params  (Optional) An associative array of key value pairs
   *                          to pass to the constructor of a loaded class or as
   *                          values to views, depending on the type of resource
   *                          loaded.
   * @param  string  $alias   (Optional) The alias to be given to the loaded Class or Model.
   *
   * @return object           Returns a Splint object if only one argument is
   *                          provided.
   */
  function splint($splint, $autoload = array(), $params = null, $alias = null) {
    $splint = trim($splint, '/');
    if (!is_dir(APPPATH . "splints/$splint/")) {
      show_error("Cannot find splint '$splint'");
      return false;
    }
    if (is_array($autoload) && count($autoload) == 0 && $params == null && $alias == null) {
      return new Splint($splint);
    }
    if (is_string($autoload)) {
      if (substr($autoload, 0, 1) == "+") {
        $this->library("../splints/$splint/libraries/" . substr($autoload, 1), $params, $alias);
      } elseif (substr($autoload, 0, 1) == "*") {
        $this->model("../splints/$splint/models/" . substr($autoload, 1), ($params != null && is_string($params) ? $params : null));
      } elseif (substr($autoload, 0, 1) == "-") {
        if ($alias === null) $alias = false;
        if ($alias) {
          return $this->view("../splints/$splint/views/" . substr($autoload, 1), $params, true);
        } else {
          $this->view("../splints/$splint/views/" . substr($autoload, 1), $params);
        }
      } elseif (substr($autoload, 0, 1) == "@") {
        $this->config("../splints/$splint/config/" . substr($autoload, 1));
      } elseif (substr($autoload, 0, 1) == "%") {
        $this->helper("../splints/$splint/helpers/" . substr($autoload, 1));
      } else {
        show_error("Resource type not specified for '$autoload', Use as prefix,
        + for Libraries, * for Models, - for Views, @ for Configs, and % for
        Helpers. e.g '+$autoload' to load the '$autoload.php' class as a library
        from the specified splint package '$splint'.");
        return false;
      }
      return true;
    }

    // We recieved an array.
    foreach ($autoload as $type => $arg) {
      if ($type == 'library') {
        if (is_array($arg)) {
          $this->library("../splints/$splint/libraries/" . $arg[0], isset($arg[1]) ? $arg[1] : null, isset($arg[2]) ? $arg[2] : null);
        } else {
          $this->library("../splints/$splint/libraries/$arg");
        }
      } elseif ($type == 'model') {
        if (is_array($arg)) {
          $this->model("../splints/$splint/models/" . $arg[0], (isset($arg[1]) ? $arg[1] : null));
        } else {
          $this->model("../splints/$splint/models/$arg");
        }
      } elseif ($type == 'config') {
        $this->config("../splints/$splint/config/$arg");
      } elseif ($type == 'helper') {
        $this->helper("../splints/$splint/helpers/$arg");
      } elseif($type == 'view') {
        $this->view("../splints/$splint/views/$arg");
      } else {
        show_error ("Could not autoload object of type '$type' for splint $splint");
      }
      return true;
    }
  }
  /**
   * package This function loads a package by loading the resources specified in
   *         package descriptor as specified.
   *
   * @param  string $splint The qualified name of the Splint package e.g.
   *                        zoey/bootstrap.
   *
   * @return int            The number of resources loaded. This function
   *                        returns false if the specified package doesn't
   *                        exist.
   */
  function package($splint) {
    $splint = trim($splint, '/');
    if (!is_dir(APPPATH . "splints/$splint/")) {
      show_error("Cannot find splint '$splint'");
      return false;
    }

    // Get Descriptor
    $descriptor = json_decode(file_get_contents(APPPATH . "splints/$splint/splint.json"));
    $loadedCount = 0;

    // Begin Check.
    if (isset($descriptor->autoload)) {
      // Libraries.
      if (isset($descriptor->autoload->libraries) && is_array($descriptor->autoload->libraries)) {
        foreach ($descriptor->autoload->libraries as $parameters) {
          if (count($parameters) == 3) {
            if (is_string($parameters[1]) && substr($parameters[1], 0, 1) == "@") {
              $this->config(substr($parameters[1], 1), true, true);
              $ci =& get_instance();
              $params = $ci->config->item(substr($parameters[1], 1), substr($parameters[1], 1));
              $params["autoload"] = true;
              $this->library("../splints/$splint/libraries/" . $parameters[0], $params, $parameters[2]);
              ++$loadedCount;
            } else {
              if (!is_scalar($parameters[1])) $params = json_decode(json_encode($parameters[1]), true);
              if (isset($params)) $params["autoload"] = true;
              $this->library("../splints/$splint/libraries/" . $parameters[0], (isset($params) && $this->is_assoc($params) ? $params : null), $parameters[2]);
              ++$loadedCount;
            }
          } else {
            $this->library("../splints/$splint/libraries/" . $parameters[0], null, $parameters[1]);
            ++$loadedCount;
          }
        }
      }
      // Models.
      if (isset($descriptor->autoload->models) && is_array($descriptor->autoload->models)) {
        foreach ($descriptor->autoload->models as $parameters) {
          if (is_string($parameters[0])) {
            $this->model("../splints/$splint/models/" . $parameters[0], isset($parameters[1]) && is_string($parameters[1]) ? $parameters[1] : null);
            ++$loadedCount;
          }
        }
      }
      // Helpers.
      if (isset($descriptor->autoload->helpers) && is_array($descriptor->autoload->helpers)) {
        foreach ($descriptor->autoload->helpers as $parameters) {
          if (is_string($parameters)) {
            $this->helper("../splints/$splint/helpers/" . $parameters);
            ++$loadedCount;
          }
        }
      }
      // Configs.
      if (isset($descriptor->autoload->configs) && is_array($descriptor->autoload->configs)) {
        foreach ($descriptor->autoload->configs as $parameters) {
          if (is_string($parameters)) {
            $this->config("../splints/$splint/config/" . $parameters);
            ++$loadedCount;
          }
        }
      }
    }
    return $loadedCount > 0;
  }
  /**
   * app Loads and renders a Splint Application, applications behave as required
   *     based on the current URI.
   *
   * @param  string $splint The fully qualified name of the Splint Application
   *                        Package to use.
   * @param  mixed  $data   (Optional) An associative array of parameters to
   *                        pass to the constructor of the controller called
   *                        within the application.
   *
   * @return bool           Returns true on successful loading or false
   *                        otherwise.
   */
  function app($splint, $data=null) {
    // Load App Router.
    //$this->splint("splint/platform", "+AppRouter", array("splint" => $splint) , "approuter");
    if (file_exists(APPPATH.'splints/splint/platform/libraries/AppRouter.php')) {
      include(APPPATH.'splints/splint/platform/libraries/AppRouter.php');
      $approuter = new AppRouter(array("splint" => $splint));
    } else {
      show_error("Platform Support Package not found.");
      return false;
    }
    // Include App Controller Parent Class
    if (file_exists(APPPATH.'splints/splint/platform/libraries/SplintAppController.php')) {
      include(APPPATH.'splints/splint/platform/libraries/SplintAppController.php');
    } else {
      show_error("Platform Support Package not found.");
      return false;
    }

    $ci =& get_instance();
    $e404 = FALSE;
	  $class = ucfirst($approuter->class);
	  $method = $approuter->method;

    // Checks and Flag Setting.
    if (empty($class) OR ! file_exists(APPPATH."splints/$splint/controllers/".$approuter->directory.$class.'.php'))	{
      $e404 = TRUE;
    }	else {
      // Require Specified Controller
      require_once(APPPATH."splints/$splint/controllers/".$approuter->directory.$class.'.php');
      // Further Checks.
      if (!class_exists($class, FALSE) OR $method[0] === '_' OR method_exists('CI_Controller', $method)) {
        $e404 = TRUE;
      }	elseif (method_exists($class, '_remap')) {
        $params = array($method, array_slice($ci->uri->apprsegments, 2));
			  $method = '_remap';
      } elseif (!method_exists($class, $method)) {
        $e404 = TRUE;
      } elseif (!is_callable(array($class, $method)))	{
        $reflection = new ReflectionMethod($class, $method);
        if ( ! $reflection->isPublic() OR $reflection->isConstructor())	{
          $e404 = TRUE;
        }
      }
    }

    if ($e404) {
      if (!empty($approuter->routes['404_override'])) {
        if (sscanf($approuter->routes['404_override'], '%[^/]/%s', $error_class, $error_method) !== 2)	{
          $error_method = 'index';
			  }
        $error_class = ucfirst($error_class);
        if (!class_exists($error_class, FALSE)) {
          if (file_exists(APPPATH."splints/$splint/controllers/".$approuter->directory.$error_class.'.php')) {
            require_once(APPPATH."splints/$splint/controllers/".$approuter->directory.$error_class.'.php');
					  $e404 = ! class_exists($error_class, FALSE);
				  }	elseif (!empty($approuter->directory) && file_exists(APPPATH."splints/$splint/controllers/".$error_class.'.php'))	{
            require_once(APPPATH."splints/$splint/controllers/".$error_class.'.php');
				 	  if (($e404 = ! class_exists($error_class, FALSE)) === FALSE) {
              $approuter->directory = '';
					  }
				  }
			  }	else {
				  $e404 = FALSE;
			  }
		  }
		  // Did we reset the $e404 flag? If so, set the rsegments, starting from index 1
		  if (!$e404)	{
        $class = $error_class;
			  $method = $error_method;
			  $ci->uri->apprsegments = array(
				  1 => $class,
				  2 => $method
			  );
		  } else {
			  show_404($approuter->directory.$class.'/'.$method);
		  }
	  }

	  if ($method !== '_remap')	{
		  $params = array_slice($ci->uri->apprsegments, 2);
	  }

    // TODO: pre-hook
    //$EXT->call_hook('pre_controller');

    // Mark a start point so we can benchmark the app controller
    $ci->benchmark->mark('app_controller_execution_time_( '.$class.' / '.$method.' )_start');

    $APP = new $class($splint, is_array($data) && $this->is_assoc($data) ? $data : null);

    // TODO: post-hook
    //$EXT->call_hook('post_controller_constructor');

    // Call App Controller.
    call_user_func_array(array(&$APP, $method), $params);

    // Mark an end point so we can benchmark the app controller
    $ci->benchmark->mark('app_controller_execution_time_( '.$class.' / '.$method.' )_end');
  }
  /**
   * [_ci_autoloader The Ripped Code Igniter autoloader modified at the bottom
   *                 to load Splint packages.
   *
   * @return null
   */
  function _ci_autoloader() {
    if (file_exists(APPPATH.'config/autoload.php')) {
			include(APPPATH.'config/autoload.php');
		}
		if (file_exists(APPPATH.'config/'.ENVIRONMENT.'/autoload.php'))	{
			include(APPPATH.'config/'.ENVIRONMENT.'/autoload.php');
		}
    if (!isset($autoload)) {return;}
		// Autoload packages
		if (isset($autoload['packages']))	{
			foreach ($autoload['packages'] as $package_path) {
				$this->add_package_path($package_path);
			}
		}
		// Load any custom config file
		if (count($autoload['config']) > 0)	{
			foreach ($autoload['config'] as $val)	{
				$this->config($val);
			}
		}
		// Autoload helpers and languages
		foreach (array('helper', 'language') as $type) {
			if (isset($autoload[$type]) && count($autoload[$type]) > 0)	{
				$this->$type($autoload[$type]);
			}
		}
		// Autoload drivers
		if (isset($autoload['drivers'])) {
			$this->driver($autoload['drivers']);
		}
		// Load libraries
		if (isset($autoload['libraries']) && count($autoload['libraries']) > 0)	{
			// Load the database driver.
			if (in_array('database', $autoload['libraries'])) {
				$this->database();
				$autoload['libraries'] = array_diff($autoload['libraries'], array('database'));
			}
			// Load all other libraries
			$this->library($autoload['libraries']);
		}
		// Autoload models
		if (isset($autoload['model'])) {
			$this->model($autoload['model']);
		}
    // Autoload splints
    if (isset($autoload["splint"])) {
      foreach ($autoload["splint"] as $splint => $res) {
        $this->splint($splint, isset($res[0]) ? $res[0] : array(),
        isset($res[1]) ? $res[1] : null, isset($res[2]) ? $res[2] : null);
      }
    }
    // Autoload splints from splint descriptors.
    if (isset($autoload["splint+"])) {
      foreach ($autoload["splint+"] as $splint) {
        $this->package($splint);
      }
    }
  }
  /**
   * [bind   creates a Splint object and optionally binds the object to a passed
   *         variable]
   *
   * @param  string $splint Splint package name.
   * @param  object $bind   Optional variable to bind Splint object to.
   *
   * @return object         [Optional] Returns a Splint oject if no argument is
   *                        passed for $bind.
   */
  function bind($splint, &$bind=null) {
    if (func_num_args() == 2) {
      $bind = new Splint($splint);
    } else {
      return new Splint($splint);
    }
  }
  /**
   * [test description]
   * @param  [type]  $splint [description]
   * @param  boolean $strict [description]
   * @return [type]          [description]
   */
  function test($splint, $strict=true) {
    // Form hidden field to determine if form parameters have been loaded.
    defined("TEST_STATUS") OR define("TEST_STATUS", "test_status");
    if (!is_dir(APPPATH . "splints/$splint/")) {
      show_error("Cannot find splint '$splint'");
      return false;
    }
    $this->helper("file");
    $test_classes = array();
    $testDir = APPPATH . "splints/$splint/tests/";
    if (!is_dir($testDir)) return true;
    $jsTestDir = $testDir . "js/";
    $scannedFiles = array_diff(scandir($testDir), array('.', '..'));
    $files = array();
    $scannedJsFiles = is_dir($jsTestDir) ? array_diff(scandir($jsTestDir), array('.', '..')) : array();
    $jsFiles = array();
    foreach ($scannedFiles as $file) {
      if (is_file($testDir . $file) && $this->endsWith($file, ".php")) $test_classes[] = $file;
    }
    foreach ($scannedJsFiles as $file) {
      if (is_file($jsTestDir . $file ) && $this->endsWith($file, ".php")) $jsTests[] = $file;
    }
    if (count($test_classes) == 0 && count($jsTests) == 0) return true;
    $ci =& get_instance();
    $platform = $this->splint("splint/platform");
    if (file_exists(APPPATH . "splints/$splint/tests/post_data.json") &&
    $ci->security->xss_clean($ci->input->post(TEST_STATUS)) == "") {
      $post_data = json_decode(file_get_contents(APPPATH . "splints/$splint/tests/post_data.json"), true);
      $post_data[TEST_STATUS] = "ready";
      $platform->load->view("form", array("fields" => $post_data));
      return true;
    }
    if (isset($jsTests) && count($jsTests) > 0) {
      $platform->load->view("js/qunit");
      $platform->load->view("css/qunit");
      $platform->load->view("qunit_container");
      foreach ($jsTests as $test) {
        $this->view("../splints/$splint/tests/js/$test");
      }
    }
    $this->library("unit_test");
    $ci->unit->use_strict($strict);
    if (count($test_classes) == 0) return false;
    $total_tests = 0;
    $test_metrics = array();
    for ($x = 0; $x < count($test_classes); $x++) {
      $this->library("../splints/$splint/tests/" . $test_classes[$x],
      null, "test$x");
      $methods = get_class_methods($ci->{"test$x"});
      foreach ($methods as $method) {
        $ci->{"test$x"}->{$method}($ci);
        $test_metrics[] = array(
          str_replace(".php", "", $test_classes[$x]),
          "$method()",
          count($ci->unit->result()) - $total_tests,
          count($ci->unit->result())
        );
        $total_tests = count($ci->unit->result());
      }
    }
    $this->displayAnalytics($test_metrics, $ci->unit->result(), count($test_classes));
  }
  /**
   * [is_assoc description]
   * @param  [type]  $arr [description]
   * @return boolean      [description]
   */
  private function is_assoc($arr) {
    return array_keys($arr) !== range(0, count($arr) - 1);
  }
  /**
   * [endsWith description]
   * @param  [type] $haystack [description]
   * @param  [type] $needle   [description]
   * @return [type]           [description]
   */
  private function endsWith($haystack, $needle) {
    $length = strlen($needle);
    if ($length == 0) return true;
    return substr($haystack, -$length) === $needle;
  }
  /**
   * [displayAnalytics description]
   * @param  [type] $report [description]
   * @param  [type] $offset [description]
   * @return [type]         [description]
   */
  private function displayAnalytics($metrics, $reports, $classes) {
    $testCount = count($reports);
    $passedCount = 0;
    $failedCount = 0;
    $this->bind("splint/platform", $platform);
    for ($x = 0; $x < $testCount; $x++) {
      if ($reports[$x]["Result"] === "Passed") {
        ++$passedCount;
      } else {
        ++$failedCount;
      }
    }
    $data = array(
      "class"        => "Overall Test Results",
      "test_count"   => count($reports),
      "passed_count" => $passedCount,
      "failed_count" => $failedCount,
      "classes"      => $classes,
      "functions"    => count($metrics)
    );
    $platform->load->view("analytics", $data);
    $platform->load->view("border", null);
    $offset = 0;
    foreach ($metrics as $metric) {
      $passedCount = 0;
      $failedCount = 0;
      for ($x = $offset; $x < $metric[3]; $x++) {
        if ($reports[$x]["Result"] === "Passed") {
          ++$passedCount;
        } else {
          ++$failedCount;
        }
      }
      $data = array(
        "class"        => $metric[0],
        "function"     => $metric[1],
        "test_count"   => $metric[2],
        "passed_count" => $passedCount,
        "failed_count" => $failedCount,
        "classes"      => "",
        "functions"    => ""
      );
      $platform->load->view("analytics", $data);
      for ($x = $offset; $x < $metric[3]; $x++) {
        $platform->load->load->view("result", array("result" => $reports[$x]));
      }
      $platform->load->view("border", null);
      $offset += $metric[2];
    }
  }
}

/**
 * [Splint description]
 */
class Splint {

  /**
   * [private description]
   * @var [type]
   */
  private $ci;
  /**
   * [private description]
   * @var [type]
   */
  private $splint;

  /**
   * [$load description]
   * @var [type]
   */
  var $load;

  /**
   * [protected description]
   * @var [type]
   */
  protected $dynamic_fields;

  function __construct($splint) {
    $this->ci =& get_instance();
    $this->splint = $splint;
    $this->load =& $this;
  }
  /**
   * [library description]
   * @param  [type] $lib    [description]
   * @param  [type] $params [description]
   * @param  [type] $alias  [description]
   * @return [type]         [description]
   */
  function library($lib, $params=null, $alias=null, $bind=false) {
    $this->ci->load->library("../splints/$this->splint/libraries/" . $lib, $params, $alias);
    if ($bind) {
      if ($alias != null && is_string($alias)) {
        $this->{$alias} =& $this->ci->{$alias};
      } else {
        $this->{strtolower($lib)} =& $this->ci->{strtolower($lib)};
      }
    }
  }
  /**
   * [view description]
   * @param  [type]  $view   [description]
   * @param  [type]  $params [description]
   * @param  boolean $return [description]
   * @return [type]          [description]
   */
  function view($view, $params=null, $return=false) {
    if ($return) {
      return $this->ci->load->view("../splints/$this->splint/views/" . $view, $params, true);
    }
    $this->ci->load->view("../splints/$this->splint/views/" . $view, $params);
  }
  /**
   * [model description]
   * @param  [type] $model [description]
   * @param  [type] $alias [description]
   * @return [type]        [description]
   */
  function model($model, $alias=null) {
    $this->ci->load->model("../splints/$this->splint/models/" . $model, $alias);
  }
  /**
   * [helper description]
   * @param  [type] $helper [description]
   * @return [type]         [description]
   */
  function helper($helper) {
    $this->ci->load->helper("../splints/$this->splint/helpers/$helper");
  }
  /**
   * [config description]
   * @param  [type] $config [description]
   * @return [type]         [description]
   */
  function config($config) {
    $this->ci->load->config("../splints/$this->splint/config/$config");
  }
}
?>
