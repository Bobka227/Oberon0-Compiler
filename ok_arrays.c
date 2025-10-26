#include <stdio.h>
#include <string.h>
#include <math.h>

static void __print_bool(int b){ printf(b?"TRUE":"FALSE"); }
static void __read_bool(int* b){ char buf[8]; if (scanf("%7s", buf)==1){ *b = (strcmp(buf,"TRUE")==0); } }


int m[2][3];
double r[2][3];

int main(void){
  m[0][1] = 42;
  /* cannot print arrays */; printf("\n");
  r[1][2] = 3.5;
  /* cannot print arrays */; printf("\n");
  /* cannot print arrays */; printf("\n");
  return 0;
}
