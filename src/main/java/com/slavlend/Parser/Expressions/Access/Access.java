package com.slavlend.Parser.Expressions.Access;

/*
Абстракция доступа (акссеса)
 */
public interface Access {
    abstract void setNext(Access access);
    abstract void setLast(Access access);
    abstract boolean hasNext();
    abstract Access getNext();
    abstract void compile(boolean hasPrevious, boolean isStatement);
    abstract Access copy();
}
