<?php
$name = "Kuba";
function greet($s) {
  $a = 42;
  return "Hello, " . $s . " " . $a . "!";
};
echo "Initializing..." . "\n";
echo "\n";
echo greet($name) . "\n";
$name = "Test";
echo greet($name) . "\n";
