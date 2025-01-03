package com.slavlend.Parser.Statements;

import com.slavlend.App;
import com.slavlend.Exceptions.PolarThrowable;
import com.slavlend.Parser.Address;
import com.slavlend.Parser.Expressions.Expression;

import java.util.ArrayList;

/*
Троу стейтмент - выкидывает throwable
 */
public class ThrowStatement implements Statement {
    // выкидываемое
    private Expression throwableExpr;
    // адресс
    private Address address = App.parser.address();

    @Override
    public void optimize() {
        // ...
    }

    @Override
    public void execute() {
        // оптимизурем
        optimize();
        // выкидываем
        throw new PolarThrowable(throwableExpr.evaluate());
    }

    @Override
    public void interrupt() {

    }

    // копирование

    @Override
    public Statement copy() {
        return new ThrowStatement(throwableExpr);
    }

    // адресс
    @Override
    public Address address() {
        return address;
    }

    // конструктор
    public ThrowStatement(Expression throwableExpr) {
        this.throwableExpr = throwableExpr;
    }
}