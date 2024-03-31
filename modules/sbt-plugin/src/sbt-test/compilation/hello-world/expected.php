<?php
//
// scala.php stdlib start
//
class scala_Unit implements Stringable{
  public function __toString() {
    return "()";
  }

  public static function consume() {
    return self::getInstance();
  }

  private static $instance = null;
  public static function getInstance() {
    if (self::$instance === null) {
      self::$instance = new self();
    }
    return self::$instance;
  }
}

function java_nio_file_Files_readString($path) {
  $str = file_get_contents($path);
  if ($str === false) {
    throw new Exception("Failed to read file: " . $path);
  }
  return $str;
}

function java_nio_file_Files_writeString($path, $str) {
  file_put_contents($path, $str);
}

//
// scala.php stdlib end
//

$s = "world";
echo "hello " . $s . "!" . "\n";
