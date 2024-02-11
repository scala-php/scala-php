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

class Data {
  public $s;
  public $i;
  private $x;
  function __construct($s, $i, $x) {
    $this -> s = $s;
    $this -> i = $i;
    $this -> x = $x;
  }
  function printed() {
    global $s, $i, $x;
    return $s . $i . $x;
  }
};
$Data = new DataDOLLAR();
class DataDOLLAR {

  function __construct() {

  }

};
$d = new Data("hello", 42, 52);
echo $d->printed() . "\n";
