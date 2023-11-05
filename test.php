<?php

$comma = "";
function demo($s)  { return $s.$comma.$s; };

$comma = ", ";
echo demo("hello");
