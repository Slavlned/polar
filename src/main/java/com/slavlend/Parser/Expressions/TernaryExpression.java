package com.slavlend.Parser.Expressions;

import com.slavlend.App;
import com.slavlend.Polar.PolarValue;
import com.slavlend.Parser.Address;
import lombok.Getter;

/*
Намбер экспрешенн - возвращает число
 */
@Getter
public class TernaryExpression implements Expression {
    // правое выражение
    private final Expression right;
    // условный оператор
    private final ConditionExpression condExpr;
    // левое выражение
    private final Expression left;

    // адресс
    private final Address address = App.parser.address();

    @Override
    public PolarValue evaluate() {
        return condExpr.evaluate().asBool() ? left.evaluate() : right.evaluate();
    }

    @Override
    public Address address() {
        return address;
    }

    public TernaryExpression(ConditionExpression condExpr, Expression left, Expression right) {
        this.condExpr = condExpr; this.left = left; this.right = right;
    }
}
