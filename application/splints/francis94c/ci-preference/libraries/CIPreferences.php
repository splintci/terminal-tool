<?php
defined('BASEPATH') OR exit('No direct script access allowed');

class CIPreferences {

  /**
   * [private description]
   * @var [type]
   */
  private $file_name = "ci_preference";
  /**
   * [private description]
   * @var [type]
   */
  private $prefs;
  /**
   * [private description]
   * @var [type]
   */
  private $pref_directory = __DIR__ . "/preferences/";
  /**
   * [private description]
   * @var [type]
   */
  private $index_file_name = "index.html";
  /**
   * [__construct description]
   */
  function __construct() {
    if (func_num_args() > 0) {
      $params = func_get_arg(0);
      if (isset($params["file_name"]) && is_string($params["file_name"])) {
        $this->file_name = $params["file_name"];
      }
      if (isset($params["index_file_name"]) && is_string($params["index_file_name"])) {
        $this->index_file_name = $params["index_file_name"];
      }
    }
    if (!is_dir($this->pref_directory)) {
      mkdir($this->pref_directory, 644);
      file_put_contents($this->pref_directory . $this->index_file_name,
      "<!DOCTYPE html>" . PHP_EOL .
      "<html>" . PHP_EOL .
      "<head>" . PHP_EOL .
      "<title>403 Forbidden</title>" . PHP_EOL .
      "</head>" . PHP_EOL .
      "<body>" . PHP_EOL .
      "<p>Directory access is forbidden.</p>" . PHP_EOL .
      "</body>" . PHP_EOL .
      "</html>");
    }
    if (is_file($this->pref_directory . $this->file_name)) {
      $this->prefs = json_decode(file_get_contents($this->pref_directory . $this->file_name), true);
    } else {
      $handle = fopen($this->pref_directory . $this->file_name, "w");
      fclose($handle);
      $this->prefs = array();
    }
  }
  /**
   * [getFileName description]
   * @return [type] [description]
   */
  function getFileName() {
    return $this->file_name;
  }
  /**
   * [set description]
   * @param [type] $key   [description]
   * @param [type] $value [description]
   */
  function set($key, $value) {
    $this->prefs[$key] = $value;
  }
  /**
   * [commit description]
   * @return [type] [description]
   */
  function commit() {
    file_put_contents($this->pref_directory . $this->file_name, json_encode($this->prefs));
  }
  /**
   * [get description]
   * @param  [type] $key     [description]
   * @param  [type] $default [description]
   * @return [type]          [description]
   */
  function get() {
    if (func_num_args() == 0) return;
    if (!is_string(func_get_arg(0))) return;
    if (isset($this->prefs[func_get_arg(0)])) return $this->prefs[func_get_arg(0)];
    if (func_num_args() == 2) return func_get_arg(1);
    return "";
  }
}
?>
