<?php
$name = "Kuba";
function greet($s) {
  $a = 42;
  return "Hello, " . $s . " " . $a . "!";
};
echo greet($name) . "\n";
echo "\n";
$name = "Test";
echo greet($name) . "\n";
