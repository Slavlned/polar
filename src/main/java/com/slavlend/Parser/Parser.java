package com.slavlend.Parser;

import com.slavlend.Polar.Stack.Classes;
import com.slavlend.Lexer.TokenType;
import com.slavlend.Parser.Expressions.*;
import com.slavlend.Lexer.Token;
import com.slavlend.Parser.Expressions.Access.*;
import com.slavlend.Parser.Statements.*;
import com.slavlend.Parser.Statements.Match.CaseStatement;
import com.slavlend.Parser.Statements.Match.DefaultStatement;
import com.slavlend.Parser.Statements.Match.MatchStatement;
import com.slavlend.Parser.Statements.Mutable.*;
import com.slavlend.Env.PolarEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
Парсер токенов в -> AST
 */
public class Parser {
    // токены
    private final List<Token> tokens;
    // текущий токен
    private int current = 0;
    // библиотеки
    public static HashMap<String, String> libraries = new HashMap<>() {{
        put("lib.random", "Libraries/random.polar");
        put("lib.array", "Libraries/array.polar");
        put("lib.map", "Libraries/map.polar");
        put("lib.telegram", "Libraries/telegram.polar");
        put("lib.tests", "Libraries/tests.polar");
        put("lib.log", "Libraries/logger.polar");
        put("lib.files", "Libraries/files.polar");
        put("lib.sys", "Libraries/sys.polar");
        put("lib.json", "Libraries/json.polar");
        put("lib.tasks", "Libraries/tasks.polar");
        put("lib.str", "Libraries/str.polar");
        put("lib.math", "Libraries/math.polar");
        put("lib.polar", "Libraries/polar.polar");
        put("lib.console", "Libraries/console.polar");
        put("lib.time", "Libraries/time.polar");
        put("lib.crypto", "Libraries/crypto.polar");
        put("lib.http", "Libraries/http.polar");
        put("lib.http.server", "Libraries/httpserver.polar");
        put("lib.graphics", "Libraries/graphics.polar");
    }};

    // путь эвайронмента (окружения)
    private String environmentPath;

    // конструктор
    public Parser(List<Token> _tokens) {
        this.tokens = _tokens;
    }

    // адресс
    public Address address() {
        if (tokens.size() == current) return new Address(-1);
        return new Address(tokens.get(current).line);
    }

    // парсинг (экзекьют)
    public void execute() {
        try {
            BlockStatement statement = new BlockStatement();

            // парсим стэйтменты
            while (current < tokens.size()) {
                statement.add(statement());
            }

            // экзекьютим
            statement.execute();
        } catch (Exception e) {
            PolarEnv.Crash("Parsing Error: " + e.getMessage(), address());
        }
    }

    public Operator condOperator() {
        // перебор операторов
        return switch (tokens.get(current).type) {
            case EQUAL -> new Operator(consume(TokenType.EQUAL).value);
            case NOT_EQUAL -> new Operator(consume(TokenType.NOT_EQUAL).value);
            case LOWER -> new Operator(consume(TokenType.LOWER).value);
            case BIGGER -> new Operator(consume(TokenType.BIGGER).value);
            case LOWER_EQUAL -> new Operator(consume(TokenType.LOWER_EQUAL).value);
            case BIGGER_EQUAL -> new Operator(consume(TokenType.BIGGER_EQUAL).value);
            case IS -> new Operator(consume(TokenType.IS).value);
            default -> {
                PolarEnv.Crash("Invalid Conditional Operator: " + tokenInfo(), new Address(tokens.get(current).line));
                yield null;
            }
        };
    }

    public Expression conditional() {
        // левый экспрешен
        Expression _l = additive();
        // если это кондишенал то парсим
        if (check(TokenType.LOWER_EQUAL) || check(TokenType.BIGGER_EQUAL) || check(TokenType.EQUAL) ||
            check(TokenType.NOT_EQUAL) || check(TokenType.BIGGER_EQUAL) || check(TokenType.LOWER) || check(TokenType.IS)) {
            // оператор
            Operator _o = condOperator();
            // правый экспрешен
            Expression _r = additive();
            // кондишен
            return new ConditionExpression(_l, _o, _r);
        }
        // в ином случае возвращаем
        return _l;
    }

    // парсинг акссесса
    public Access accessPart() {
        // адресс
        Address _address = address();
        // дальнейшие действия
        if (check(TokenType.ID)) {
            // айди
            String name = consume(TokenType.ID).value;
            // функция
            if (check(TokenType.BRACKET) && match("(")) {
                // параметры
                ArrayList<Expression> params = new ArrayList<>();
                // аргументы
                consume(TokenType.BRACKET);

                while (!match(")")) {
                    if (!check(TokenType.COMMA)) {
                        params.add(parseExpression());
                    }
                    else {
                        consume(TokenType.COMMA);
                    }
                }

                consume(TokenType.BRACKET);
                return new CallAccess(_address, null, name, params);
            }
            // переменная
            else {
                return new VarAccess(_address, null, name);
            }
        }
        else {
            // new объект
            return new TempAccess(_address, null, (ObjectExpression) objectExpr());
        }
    }

    // получение айди переменной
    public AccessExpression parseAccess() {
        AccessExpression expr = new AccessExpression(address(), null);

        // добавляем акссесс
        expr.Add(accessPart());

        while (check(TokenType.DOT)) {
            // консьюм
            consume(TokenType.DOT);
            // добавляем акссесс
            expr.Add(accessPart());
        }

        return expr;
    }

    // парсим функции
    public Statement function() {
        // паттерн
        // func (...) = { ... }
        consume(TokenType.FUNC);
        // имя функции
        String name = consume(TokenType.ID).value;
        // скобка
        consume(TokenType.BRACKET);
        // парсим аргументы функции
        ArrayList<ArgumentExpression> arguments = new ArrayList<>();
        while (!check(TokenType.BRACKET)) {
            if (!check(TokenType.COMMA)) {
                arguments.add(new ArgumentExpression(consume(TokenType.ID).value));
            }
            else {
                consume(TokenType.COMMA);
            }
        }
        // создаём функции
        FunctionStatement functionStatement = new FunctionStatement(name, arguments);
        // скобки
        consume(TokenType.BRACKET);
        // ассигн
        consume(TokenType.ASSIGN);
        // брэйс
        consume(TokenType.BRACE);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            Statement statement = statement();
            functionStatement.add(statement);
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return functionStatement;
    }

    // парсим стэйтмент
    public Statement statement() {
        // вызов функции
        if (check(TokenType.CALL)) {
            return call();
        }
        // стэйтмент if
        if (check(TokenType.IF)) {
            return ifElifElse();
        }
        // стэйтмент match
        if (check(TokenType.MATCH)) {
            // мэтч
            return match();
        }
        // стэйтмент while
        if (check(TokenType.WHILE)) {
            return whileLoop();
        }
        // стэйтмент for
        if (check(TokenType.FOR)) {
            return forLoop();
        }
        // стэйтмент each
        if (check(TokenType.EACH)) {
            return eachLoop();
        }
        // стэйтмент ассерт
        if (check(TokenType.ASSERT)) {
            return polarAssert();
        }
        // стэйтмент функции
        if (check(TokenType.FUNC)) {
            FunctionStatement func = (FunctionStatement) function();
            func.putToFunction();

            return func;
        }
        // стэйтмент класса
        if (check(TokenType.CLASS)) {
            return polarClass();
        }
        // стэйтмент id
        if (check(TokenType.ID)) {
            // паттерн присваивания
            // ... = ...
            // ... += ...
            // ... -= ...
            // ... *= ...
            // ... /= ...
            AccessExpression id = parseAccess();
            // ==
            if (check(TokenType.ASSIGN)) {
                consume(TokenType.ASSIGN);
                // expression
                Expression value = parseExpression();
                VarAccess last = ((VarAccess) id.GetLast());
                id.Set(new AssignAccess(id.address(), null, last.varName, value, AccessType.SET));
            }
            // +=
            else if (check(TokenType.ASSIGN_ADD)) {
                consume(TokenType.ASSIGN_ADD);
                // expression
                Expression value = parseExpression();
                VarAccess last = ((VarAccess) id.GetLast());
                id.Set(new AssignAccess(id.address(), null, last.varName, value, AccessType.PLUS));
            }
            // -=
            else if (check(TokenType.ASSIGN_SUB)) {
                consume(TokenType.ASSIGN_SUB);
                // expression
                Expression value = parseExpression();
                VarAccess last = ((VarAccess) id.GetLast());
                id.Set(new AssignAccess(id.address(), null, last.varName, value, AccessType.MINUS));
            }
            // *=
            else if (check(TokenType.ASSIGN_MUL)) {
                consume(TokenType.ASSIGN_MUL);
                // expression
                Expression value = parseExpression();
                VarAccess last = ((VarAccess) id.GetLast());
                id.Set(new AssignAccess(id.address(), null, last.varName, value, AccessType.MUL));
            }
            // /=
            else if (check(TokenType.ASSIGN_DIVIDE)) {
                consume(TokenType.ASSIGN_DIVIDE);
                // expression
                Expression value = parseExpression();
                VarAccess last = ((VarAccess) id.GetLast());
                id.Set(new AssignAccess(id.address(), null, last.varName, value, AccessType.DIVIDE));
            }
            else {
                // неизвестный оператор кондишена
                error("Cannot Use Id Like Statement If Its Not For Assignment Or Call: " + tokenInfo());

                return null;
            }

            return id;
        }
        // стэйтмент юза
        if (check(TokenType.USE)) {
            // юз
            consume(TokenType.USE);
            // айдишник
            TextExpression id = (TextExpression) parseExpression();
            // библиотека
            return new UseLibStatement(id);
        }
        // стэйтмент брейка
        if (check(TokenType.BREAK)) {
            // юз
            consume(TokenType.BREAK);
            // библиотека
            return new BreakStatement();
        }
        // стэйтмент некста
        if (check(TokenType.NEXT)) {
            // юз
            consume(TokenType.NEXT);
            // библиотека
            return new NextStatement();
        }
        else {
            // неизвестный токен
            error("Invalid Statement Token: " + tokenInfo());
            return null;
        }
    }

    // парсинг кейса
    private Statement matchCase() {
        // кейс
        consume(TokenType.CASE);
        // скобка
        consume(TokenType.BRACKET);
        // экспрешен
        Expression expr = parseExpression();
        // скобка
        consume(TokenType.BRACKET);
        // тело блока
        consume(TokenType.BRACE);
        // кейс стэйтмент
        CaseStatement statement = new CaseStatement(expr);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг дефолта
    private Statement matchDefault() {
        // дефолт
        consume(TokenType.DEFAULT);
        // тело функции
        consume(TokenType.BRACE);
        // дефолт стэйтмент
        DefaultStatement statement = new DefaultStatement();
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг мэтча
    private Statement match() {
        // мэтч
        consume(TokenType.MATCH);
        // скобка
        consume(TokenType.BRACKET);
        // экспрешен
        Expression expr = parseExpression();
        // скобка
        consume(TokenType.BRACKET);
        // мэтч
        MatchStatement statement = new MatchStatement(expr);
        // брейс
        consume(TokenType.BRACE);
        // кейсы
        while (check(TokenType.CASE)) {
            statement.add(matchCase());
        }
        // дефолт
        if (check(TokenType.DEFAULT)) {
            statement.add(matchDefault());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг класса
    private Statement polarClass() {
        // паттерн
        // class (...) = { ... }
        consume(TokenType.CLASS);
        // имя класса
        String name = consume(TokenType.ID).value;
        // скобка
        consume(TokenType.BRACKET);
        // конструктор
        ArrayList<ArgumentExpression> constructor = new ArrayList<>();
        // аргументы конструктора
        while (!check(TokenType.BRACKET)) {
            if (!check(TokenType.COMMA)) {
                constructor.add(new ArgumentExpression(consume(TokenType.ID).value));
            } else {
                consume(TokenType.COMMA);
            }
        }
        // стэйтменты
        ClassStatement classStatement = new ClassStatement(name, constructor);
        // скобка
        consume(TokenType.BRACKET);
        // ассигн
        consume(TokenType.ASSIGN);
        // брэйс
        consume(TokenType.BRACE);
        // тело функции
        while (!check(TokenType.BRACE)) {
            // функция
            if (check(TokenType.FUNC)) {
                // функция
                Statement statement = function();
                // добавляем в класс
                if (statement instanceof FunctionStatement) {
                    classStatement.add((FunctionStatement) statement);
                }
                else {
                    error("Cannot Use Any Statements Except Functions: " + tokenInfo());
                    return null;
                }
            }
            // модульная функция и переменная
            else if (check(TokenType.MOD)) {
                consume(TokenType.MOD);
                // модульная функция
                if (check(TokenType.FUNC)) {
                    // функция
                    Statement statement = function();
                    // добавляем
                    if (statement instanceof FunctionStatement) {
                        classStatement.addModule((FunctionStatement) statement);
                    }
                    else {
                        error("Cannot Use Any Statements Except Functions: " + tokenInfo());
                        return null;
                    }
                }
                // модульная переменная
                else {
                    // айди
                    String idData = this.consume(TokenType.ID).value;
                    // ассигн
                    consume(TokenType.ASSIGN);
                    // экспрешенн
                    Expression expr = parseExpression();
                    // добавляем модульную переменную
                    classStatement.addModuleVariable(idData, expr);
                }
            }
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращение
        return classStatement;
    }

    // парсинг while
    private Statement whileLoop() {
        // паттерн
        // while (...) { ... }
        consume(TokenType.WHILE);
        // скобка
        consume(TokenType.BRACKET);
        // кондишены
        ArrayList<ConditionExpression> _conditions = new ArrayList<>();
        while (!match(")")) {
            if (!check(TokenType.AND)) {
                _conditions.add((ConditionExpression) conditional());
            } else {
                consume(TokenType.AND);
            }
        }
        // брэкет
        consume(TokenType.BRACKET);
        // брэйкс
        consume(TokenType.BRACE);
        // стэйтмент вайл
        WhileStatement statement = new WhileStatement(_conditions);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг for
    private Statement forLoop() {
        // паттерн
        // for (... = ..., ... operator ...) { ... }
        consume(TokenType.FOR);
        // скобка
        consume(TokenType.BRACKET);
        // кондишены
        ArrayList<ConditionExpression> _conditions = new ArrayList<>();
        // переменная
        String assignVariableName = consume(TokenType.ID).value;
        // ассигн
        consume(TokenType.ASSIGN);
        // присваивание
        Expression assignVariableExpr = parseExpression();
        // запятая
        consume(TokenType.COMMA);
        // левый экспрешен
        AccessExpression _l = parseAccess();
        // оператор
        Operator _o = condOperator();
        // правый экспрешен
        Expression _r = parseExpression();
        // кондишен
        _conditions.add(new ConditionExpression(_l, _o, _r));
        // скобка
        consume(TokenType.BRACKET);
        // брэйкс
        consume(TokenType.BRACE);
        // стэйтмент вайл
        ForStatement statement = new ForStatement(_conditions, assignVariableName, assignVariableExpr);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг each
    private Statement eachLoop() {
        // паттерн
        // each (elem, lst) { ... }
        consume(TokenType.EACH);
        // скобка
        consume(TokenType.BRACKET);
        // экспрешенны
        String elem = consume(TokenType.ID).value;
        consume(TokenType.COMMA);
        AccessExpression lst = parseAccess();
        // скобка
        consume(TokenType.BRACKET);
        // брэйкс
        consume(TokenType.BRACE);
        // стэйтмент вайл
        EachStatement statement = new EachStatement(lst, elem);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг ассерта
    private Statement polarAssert() {
        // паттерн
        // assert (... = ...)
        consume(TokenType.ASSERT);
        // скобка
        consume(TokenType.BRACKET);
        // экспрешенн
        ConditionExpression expr = (ConditionExpression) conditional();
        // скобка
        consume(TokenType.BRACKET);
        // возвращаем
        return new AssertStatement(expr);
    }

    // парсинг ифа
    private Statement ifElifElse() {
        // паттерн
        // if (...) { ... }
        consume(TokenType.IF);
        // скобка
        consume(TokenType.BRACKET);
        // кондишены
        ArrayList<Expression> conditions = new ArrayList<>();
        // уровень скобок
        while (!match(")")) {
            // and
            if (check(TokenType.AND)) {
                consume(TokenType.AND);
            }
            // expr
            else {
                conditions.add(conditional());
            }
        }
        // брэкет
        consume(TokenType.BRACKET);
        // брэйс
        consume(TokenType.BRACE);
        // иф стэйтмент
        IfStatement statement = new IfStatement(conditions);
        IfStatement last = statement;
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брейс
        consume(TokenType.BRACE);
        // в иных случаях
        while (check(TokenType.ELSE) ||
                check(TokenType.ELIF)) {
            // если нет, то кондишен
            if (check(TokenType.ELIF)) {
                // элиф
                consume(TokenType.ELIF);
                // скобка
                consume(TokenType.BRACKET);
                // кондишены
                ArrayList<Expression> _conditions = new ArrayList<>();
                while (!match(")")) {
                    if (!check(TokenType.AND)) {
                        _conditions.add(conditional());
                    } else {
                        consume(TokenType.AND);
                    }
                }
                // брэкет
                consume(TokenType.BRACKET);
                // брэйс
                consume(TokenType.BRACE);
                // иф стэйтмент
                IfStatement _statement = new IfStatement(_conditions);
                // тело функции
                while (!check(TokenType.BRACE)) {
                    _statement.add(statement());
                }
                // брэйс
                consume(TokenType.BRACE);
                // добавляем в цепочку
                last._else = _statement;
                last = _statement;
            }
            else {
                // когда ни одно условие не сработало
                consume(TokenType.ELSE);
                // брэйс
                consume(TokenType.BRACE);
                // кондишены
                ArrayList<Expression> conditionalExpressions = new ArrayList<>();
                conditionalExpressions.add(new ConditionExpression(new NumberExpression("0"), new Operator("=="), new NumberExpression("0")));
                // стэйтмент
                IfStatement _statement = new IfStatement(conditionalExpressions);
                // стэйтменты
                while (!check(TokenType.BRACE)) {
                    _statement.add(statement());
                }
                // брэйс
                consume(TokenType.BRACE);
                // добавляем в цепочку
                last._else = _statement;
                last = _statement;
            }
        }
        // возвращаем
        return statement;
    }

    public Statement call() {
        // паттерн
        // @%func%
        // or
        // @%object%.%func%
        // колл
        // колл
        consume(TokenType.CALL);
        // айди или new (юзер дефайнед функции)
        if (check(TokenType.ID) || check(TokenType.NEW)) {
            return parseAccess();
        }
        // системные функции
        else {
            // back
            if (check(TokenType.BACK)) {
                // паттерн @back(expr)
                consume(TokenType.BACK);
                consume(TokenType.BRACKET);
                Expression e = parseExpression();
                consume(TokenType.BRACKET);
                return new BackStatement(e);
            }
            // to int
            if (check(TokenType.TONUM)) {
                // паттерн @int(expr)
                consume(TokenType.TONUM);
                consume(TokenType.BRACKET);
                Expression e = parseExpression();
                consume(TokenType.BRACKET);
                return new NumStatement(e);
            }
            // to str
            if (check(TokenType.TOSTR)) {
                // паттерн @string(expr)
                consume(TokenType.TOSTR);
                consume(TokenType.BRACKET);
                Expression e = parseExpression();
                consume(TokenType.BRACKET);
                return new StrStatement(e);
            }
            // to bool
            if (check(TokenType.TOBOOL)) {
                // паттерн @bool(expr)
                consume(TokenType.TOBOOL);
                consume(TokenType.BRACKET);
                Expression e = parseExpression();
                consume(TokenType.BRACKET);
                return new BoolStatement(e);
            }
        }

        // функция не найдена
        error("Function Not Found: " + tokenInfo());
        // возвращаем
        return null;
    }

    // парсинг создания объекта
    public Expression objectExpr() {
        // паттерн
        // ... = new %class%()
        consume(TokenType.NEW);
        // айди
        String clazz = consume(TokenType.ID).value;
        // брэкет
        consume(TokenType.BRACKET);
        // параметры
        ArrayList<Expression> params = new ArrayList<>();
        // конструктор
        while (!match(")")) {
            if (!check(TokenType.COMMA)) {
                params.add(parseExpression());
            }
            else {
                consume(TokenType.COMMA);
            }
        }
        // брэкет
        consume(TokenType.BRACKET);
        // экспрешенн
        return new ObjectExpression(clazz, params);
    }

    // Парсинг рефлексийного выражения
    private Expression reflectExpr() {
        // рефлексия
        consume(TokenType.REFLECT);
        // имя класса
        String className = consume(TokenType.TEXT).value;
        // рефлексийный экспрешенн
        return new ReflectExpression(className);
    }

    // Парсинг сложения и вычитания
    private Expression additive() {
        Expression expr = multiplicative();

        while (check(TokenType.OPERATOR) && (match("+") || match("-"))) {
            Operator operator = new Operator(consume(TokenType.OPERATOR).value);
            Expression right = multiplicative();
            expr = new ArithmeticExpression(expr, operator, right);
        }

        return expr;
    }

    // Парсинг умножения и деления
    private Expression multiplicative() {
        Expression expr = parsePrimary();

        while (check(TokenType.OPERATOR) && (match("*") || match("/") || match("%"))) {
            Operator operator = new Operator(consume(TokenType.OPERATOR).value);
            Expression right = parsePrimary();
            expr = new ArithmeticExpression(expr, operator, right);
        }

        return expr;
    }

    // парсинг экспрешенна
    public Expression parseExpression() {
        return conditional();
    }

    // парсинг праймари экспрешенна
    public Expression parsePrimary() {

        // Проверка вызова функции
        if (check(TokenType.CALL)) {
            return (Expression) call();
        }
        // Проверка создания объекта
        if (check(TokenType.NEW)) {
            return objectExpr();
        }
        // Обработка идентификаторов
        if (check(TokenType.ID)) {
            return parseAccess();
        }
        // Обработка текстовых литералов
        if (check(TokenType.TEXT)) {
            return new TextExpression(consume(TokenType.TEXT).value);
        }
        // Обработка числовых литералов
        if (check(TokenType.NUM)) {
            return new NumberExpression(consume(TokenType.NUM).value);
        }
        // Обработка ниллевых(null) литералов
        if (check(TokenType.NIL)) {
            consume(TokenType.NIL);
            return new NilExpression();
        }
        // Обработка рефлексии
        if (check(TokenType.REFLECT)) {
            return reflectExpr();
        }
        // Обработка булевых литералов
        if (check(TokenType.BOOL)) {
            return new BoolExpression(consume(TokenType.BOOL).value);
        }
        // Обработка скобочных выражений
        if (check(TokenType.BRACKET)) {
            consume(TokenType.BRACKET);  // Открывающая скобка
            Expression expression = parseExpression();  // Рекурсивный вызов parseExpression
            consume(TokenType.BRACKET);  // Закрывающая скобка
            return expression;
        }
        // Обработка контейнерных выражений
        if (check(TokenType.Q_BRACKET)) {
            consume(TokenType.Q_BRACKET);  // Открывающая скобка
            ArrayList<Expression> expressions = parseList();  // Получаем контейнер
            consume(TokenType.Q_BRACKET);  // Закрывающая скобка
            return new ContainerExpression(expressions);
        }
        // Обработка контейнерных ассациативных выражений
        if (check(TokenType.BRACE)) {
            consume(TokenType.BRACE);  // Открывающая скобка
            HashMap<Expression, Expression> expressions = parseMapContainer(); // Получаем контейнер
            consume(TokenType.BRACE);  // Закрывающая скобка
            return new MapContainerExpression(expressions);
        }
        // Обработка тернарных выражений
        if (check(TokenType.TERNARY)) {
            consume(TokenType.TERNARY); // Вопросительный знак
            consume(TokenType.BRACKET); // Открывающая скобка
            Expression expr = conditional();
            consume(TokenType.BRACKET); // Закрывающая скобка
            consume(TokenType.BRACKET); // Открывающая скобка
            Expression lExpr = parseExpression();
            consume(TokenType.COMMA); // Запятая
            Expression rExpr = parseExpression();
            consume(TokenType.BRACKET); // Закрывающая скобка
            return new TernaryExpression((ConditionExpression) expr, lExpr, rExpr);
        }
        // Если ни один из случаев не подходит, вызывается ошибка
        error("Invalid Token While Primary Parse: " + tokenInfo());
        return null;
    }

    // парсинг мапы (словаря)
    public MapContainerExpression parseMap() {
        consume(TokenType.BRACE);  // Открывающая скобка
        HashMap<Expression, Expression> expressions = parseMapContainer(); // Получаем контейнер мапы
        consume(TokenType.BRACE);  // Закрывающая скобка
        return new MapContainerExpression(expressions);
    }

    // парсинг контейнера мапы (словаря)
    public HashMap<Expression, Expression> parseMapContainer() {
        // контейнер
        HashMap<Expression, Expression> container = new HashMap<>();

        // пока нет конца фигурной скобки -> парсим экспрешенн
        if (!match("}")) {
            // ключ
            Expression key = parseExpression();
            // двоеточие
            consume(TokenType.COLON);
            // значение
            Expression value = parseExpression();
            // помещаем
            container.put(key, value);

            // пока есть запятая -> парсим
            while (check(TokenType.COMMA)) {
                // запятая
                consume(TokenType.COMMA);
                // ключ
                key = parseExpression();
                // двоеточие
                consume(TokenType.COLON);
                // значение
                value = parseExpression();
                // помещаем контейнер
                container.put(key, value);
            }
        }

        // возвращаем
        return container;
    }

    // парсинг списка
    private ArrayList<Expression> parseList() {
        // контейнер
        ArrayList<Expression> container = new ArrayList<>();

        // пока нет конца квадратной скобки -> парсим экспрешенн
        if (!match("]")) {
            // добавляем в контейнер
            container.add(parseExpression());

            // пока есть запятая -> парсим
            while (check(TokenType.COMMA)) {
                // запятая
                consume(TokenType.COMMA);
                // экспрешенн
                container.add(parseExpression());
            }
        }

        return container;
    }

    // consume
    private Token consume(TokenType expectedType) {
        // если токен таков -> возвращаем
        if (check(expectedType)) {
            return tokens.get(current++);
        }
        // ошибка
        error("Invalid Token " + tokenInfo() + " expected (" + expectedType + ")");
        return new Token(TokenType.NIL, "nil", -1);
    }

    // проверка следующего токена
    private boolean check(TokenType type) {
        if (current < tokens.size()) {
            return tokens.get(current).type.equals(type);
        }
        return false;
    }

    // проверка на мэтч строки
    private boolean match(String expected) {
        return current < tokens.size() && tokens.get(current).value.equals(expected);
    }

    // вывод ошибки
    public void error(String message) {
        // токен
        Token token = tokens.get(current);
        // трэйс ошибки
        PolarEnv.Crash(message, new Address(token.line));
    }

    // токен инфо
    public String tokenInfo() {
        // токен
        Token token = tokens.get(current);
        // инфа токена
        return "(" + token.type + ", " + token.value + ", "  + token.line + ")" /*+ " №" + current + " L:"*/;
    }

    // загрузка классов библиотеки
    public void loadClasses()  {
        // классы
        ArrayList<ClassStatement> classes = new ArrayList<>();
        // класс
        while (current < tokens.size()) {
            // стэйтмент юз
            if (check(TokenType.USE)) {
                // юзаем
                consume(TokenType.USE);
                // получаем название библиотеки
                TextExpression id = (TextExpression) parseExpression();
                // возвращаем
                new UseLibStatement(id).execute();
            }
            // стэйтмент класс
            else if (check(TokenType.CLASS)) {
                // паттерн
                // class (...) = { ... }
                consume(TokenType.CLASS);
                // имя класса
                String name = consume(TokenType.ID).value;
                consume(TokenType.BRACKET);
                // конструктор
                ArrayList<ArgumentExpression> constructor = new ArrayList<>();
                // аргументы
                while (!check(TokenType.BRACKET)) {
                    if (!check(TokenType.COMMA)) {
                        constructor.add(new ArgumentExpression(consume(TokenType.ID).value));
                    } else {
                        consume(TokenType.COMMA);
                    }
                }
                // стэйтмент класса
                ClassStatement classStatement = new ClassStatement(name, constructor);
                // скобка
                consume(TokenType.BRACKET);
                // ассигн
                consume(TokenType.ASSIGN);
                // брэйс
                consume(TokenType.BRACE);
                // тело класса
                while (!check(TokenType.BRACE)) {
                    // функция
                    if (check(TokenType.FUNC)) {
                        // функция
                        Statement statement = function();
                        // добавляем в класс
                        if (statement instanceof FunctionStatement) {
                            classStatement.add((FunctionStatement) statement);
                        }
                        else {
                            error("Cannot Use Any Statements Except Functions, Mod Function Or Mod Variables In Class: " + tokenInfo());
                        }
                    }
                    else if (check(TokenType.MOD)) {
                        consume(TokenType.MOD);
                        // модульная функция
                        if (check(TokenType.FUNC)) {
                            Statement statement = function();
                            if (statement instanceof FunctionStatement) {
                                classStatement.addModule((FunctionStatement) statement);
                            } else {
                                error("Cannot Use Any Statements Except Functions, Mod Function Or Mod Variables In Class: " + tokenInfo());
                            }
                        }
                        // модульная переменная
                        else {
                            String assignName = consume(TokenType.ID).value;
                            consume(TokenType.ASSIGN);
                            Expression expr = parseExpression();
                            classStatement.addModuleVariable(assignName, expr);
                        }
                    }
                }
                // брэйс
                consume(TokenType.BRACE);
                // удаляем если там такой класс есть
                classes.removeIf(_clazz -> _clazz.name.equals(classStatement.name));
                // добавляем
                classes.add(classStatement);
            }
            // стэйтмент функция
            else if (check(TokenType.FUNC)) {
                // помещаем функцию
                ((FunctionStatement) function()).putToFunction();
            }
            else {
                // продвигаемся по токенам
                current++;
            }
        }
        // добавляем найденные классы в кучу
        List<ClassStatement> forRemove = new ArrayList<>();
        for (ClassStatement st: classes) {
            for (ClassStatement _st: Classes.getInstance().classes) {
                if (_st.name.equals(st.name)) {
                    forRemove.add(st);
                }
            }
        }
        Classes.getInstance().classes.removeAll(forRemove);
        Classes.getInstance().classes.addAll(classes);
    }

    // установка энвайронмента (путя из окружения)
    public void setEnv(String path) {
        this.environmentPath = path;
    }

    // получение энвайронмента (путя из окружения)
    public String getEnv() {
        return this.environmentPath;
    }
}
