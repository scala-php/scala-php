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

//
// scala.php stdlib end
//

function greet() {
  $greeting = "hello world!";
  $in = readline();
  return $greeting . ", " . $in . ". Testing the compiler plugin. this is crazy!";
}
echo greet() . "\n";