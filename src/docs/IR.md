`llvm-link llvm_ir.txt lib.ll -S -o out.ll`

ubuntu测试：

`clang -emit-llvm -S main.c -o main.ll`

`llvm-link main.ll lib.ll -S -o out.ll`