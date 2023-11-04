<?php
$greeting = "hello";
$name = "Kuba";
function modify($s) {
  $a = 42;
  return $s . " " . $a . "!";
};
echo $greeting . ", " . modify($name) . "\n";
echo "\n";
$name = "Test";
echo $greeting . ", " . $name . "\n";
