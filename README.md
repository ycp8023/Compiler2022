# Compiler2022

Writing rubbish compiler in BUAA


留念，参考就算了

生成llvm，在master分支

错误处理版，在error分支（单拿出来了）

期末上机回忆版：

1.代码生成（上机题）

增加`bitand`运算符，作用同`&`，优先级同`* / % `

文法修改：
```
MulExp → UnaryExp | MulExp ('*' | '/' | '%' | 'bitand') UnaryExp
VarDef → Ident { '[' ConstExp ']' }| Ident { '[' ConstExp ']' } '=' InitVal |   Ident '=' 'getint' '(' ')'
```
样例
```
int main()
{
	int i = getint(), j = getint();
	printf("%d", i bitand j);
	return 0;
}
```
2.错误处理（简答）
大概是类似`int b[2]={0,1,2}`要怎么处理
3.关于你设计的编译器的运行栈
