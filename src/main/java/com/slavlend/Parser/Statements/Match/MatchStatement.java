package com.slavlend.Parser.Statements.Match;

import com.slavlend.App;
import com.slavlend.Env.PolarEnv;
import com.slavlend.Polar.PolarValue;
import com.slavlend.Optimization.Optimizations;
import com.slavlend.Parser.Address;
import com.slavlend.Parser.Expressions.Expression;
import com.slavlend.Parser.Statements.Statement;

import java.util.ArrayList;

public class MatchStatement implements Statement {
    // мэтч экспрешенн
    private Expression matchExpr;
    // кейсы
    public ArrayList<Statement> statements = new ArrayList<Statement>();
    // адресс
    private Address address = App.parser.address();

    @Override
    public void optimize() {
        // оптимизируем
        optimize();
        // оптимизируем константной сверткой
        matchExpr = Optimizations.optimize(matchExpr);
    }

    @Override
    public void execute() {
        // стэйтмент дефаулт
        DefaultStatement defaultStatement = null;
        boolean _default = true;
        // стэйтменты
        for (Statement statement : statements) {
            // кейс стэйтмент
            if (statement instanceof CaseStatement) {
                // is right
                if (((CaseStatement) statement).isRight(matchExpr)) {
                    _default = false;
                }
                // value
                PolarValue _value = ((CaseStatement) statement).executeBy(matchExpr);
                if (_value != null) {
                    throw _value;
                }
            }
            // дефаулт стэйтмент
            else if (statement instanceof DefaultStatement) {
                defaultStatement = (DefaultStatement) statement;
            }
            else {
                PolarEnv.Crash("Cannot Use Any Statement Except Default | Case In Match!", address);
            }
        }

        // дефолт вызов если все кейсы не сработали
        if (_default) {
            defaultStatement.execute();
        }
    }

    public void add(Statement statement) {
        statements.add(statement);
    }

    @Override
    public void interrupt() {

    }

    // копирование

    @Override
    public Statement copy() {
        MatchStatement _copy = new MatchStatement(matchExpr);

        for (Statement statement : statements) {
            _copy.add(statement.copy());
        }

        return _copy;
    }

    @Override
    public Address address() {
        return address;
    }

    // конструктор
    public MatchStatement(Expression m) {
        this.matchExpr = m;
    }
}
