package com.slavlend.Parser.Expressions.Access;

import com.slavlend.App;
import com.slavlend.PolarLogger;
import com.slavlend.Parser.Address;
import com.slavlend.Parser.Expressions.Expression;
import com.slavlend.Parser.Statements.Statement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/*
Экспрешенн пайп-выражения
 */
@Getter
public class PipeExpression implements Expression, Statement {
    // левое и правое выражение
    private final Expression l;
    private final AccessExpression r;
    // адресс
    private final Address address = App.parser.address();

    // конструктор
    public PipeExpression(Expression l, AccessExpression r) {
        this.l = l;
        this.r = r;
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public void compile() {
        if (r.getLast() instanceof CallAccess callAccess) {
            ArrayList<Expression> params = new ArrayList<>();
            params.add(l);
            params.addAll(callAccess.getParams());
            callAccess.setParams(params);
            r.compile();
        }
        else {
            PolarLogger.exception("Not A Call Access In Pipe Expr!", address());
        }
    }
}
