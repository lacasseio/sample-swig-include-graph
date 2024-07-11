%import "bar.i"
#include "missing.h"
%{
#include "one.h"
#include "ignore.h"
#include "two.hpp"
%}
%include "far.h"
