<?php  if (! defined('BASEPATH')) exit('No direct script access allowed');

/**
 * [MY_Loader description]
 */
class MY_Loader extends CI_Loader {

  /**
   * [splint description]
   * @param  [type] $splint   [description]
   * @param  array  $autoload [description]
   * @return [type]           [description]
   */
  function splint($splint, $autoload = array(), $params = null, $alias = null) {
    $splint = trim($splint, '/');
    if (!file_exists(APPPATH . "splints/$splint/")) {
      show_error("Cannot find splint '$splint'");
    }
    if (is_string($autoload)) {
      if (substr($autoload, 0, 1) == "+") {
        $this->library("../splints/$splint/libraries/" . substr($autoload, 1), $params, $alias);
      } elseif (substr($autoload, 0, 1) == "*") {
        $this->model("../splints/$splint/models/" . substr($autoload, 1));
      } elseif (substr($autoload, 0, 1) == "-") {
        $this->view("../splints/$splint/views/" . substr($autoload, 1));
      } elseif (substr($autoload, 0, 1) == "@") {
        $this->config("../splints/$splint/config/" . substr($autoload, 1));
      } elseif (substr($autoload, 0, 1) == "%") {
        $this->helper("../splints/$splint/helpers/" . substr($autoload, 1));
      }
      return true;
    }
    foreach ($autoload as $type => $arg) {
      if ($type == 'library') {
        if (is_array($arg)) {
          $this->library("../splints/$splint/libraries/" . $arg[0], (isset($arg[1]) ? $arg[1] : null), (isset($arg[2]) ? $arg[2] : $arg[0]));
        } else {
          $this->library("../splints/$splint/libraries/$arg");
        }
      } elseif ($type == 'model') {
        $this->model("../splints/$splint/models/$arg");
      } elseif ($type == 'config') {
        $this->config("../splints/$splint/config/$arg");
      } elseif ($type == 'helper') {
        $this->helper("../splints/$splint/helpers/$arg");
      } elseif($type == 'view') {
        $this->view("../splints/$splint/views/$arg");
      } else {
        show_error ("Could not autoload object of type '$type' ($arg) for splint $splint");
      }
      return true;
    }
  }
}
