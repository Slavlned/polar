package com.slavlend.Parser;

import com.slavlend.Lexer.TokenType;
import com.slavlend.Parser.Expressions.*;
import com.slavlend.Lexer.Token;
import com.slavlend.Parser.Expressions.Access.*;
import com.slavlend.Parser.Statements.*;
import com.slavlend.Parser.Statements.Match.CaseStatement;
import com.slavlend.Parser.Statements.Match.DefaultStatement;
import com.slavlend.Parser.Statements.Match.MatchStatement;
import com.slavlend.PolarLogger;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
Парсер токенов в -> AST
 */
@SuppressWarnings("unused")
public class Parser {
    // токены
    @Getter
    private final List<Token> tokens;
    // текущий токен
    private int current = 0;
    // библиотеки
    @Getter
    public static HashMap<String, String> libraries = new HashMap<>() {{
        put("lib.strings", "Libraries/strings.polar");
        put("lib.console", "Libraries/console.polar");
        put("lib.tasks", "Libraries/tasks.polar");
        put("lib.math", "Libraries/math.polar");
        put("lib.random", "Libraries/random.polar");
        put("lib.window2d", "Libraries/window2d.polar");
        put("lib.base64", "Libraries/base64.polar");
    }};

    // путь эвайронмента (окружения)
    @Getter
    @Setter
    private String environmentPath;
    @Getter
    @Setter
    private String fileName;

    // конструктор
    public Parser(List<Token> _tokens) {
        this.tokens = _tokens;
    }

    // адресс
    public Address address() {
        if (current - 1 < 0) {
            return new Address(0);
        }
        else {
            return new Address(tokens.get(current-1).line);
        }
    }

    // парсинг
    public BlockStatement parse() {
        try {
            BlockStatement statement = new BlockStatement();

            // парсим стэйтменты
            while (current < tokens.size()) {
                statement.add(statement());
            }

            // экзекьютим и ловим ошибки
            return statement;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                PolarLogger.exception("Invalid Conditional Operator: " + tokenInfo(), new Address(tokens.get(current).line));
                yield null;
            }
        };
    }

    // парсинг кондишена
    public Expression conditional() {
        // левый экспрешен
        Expression _l = additive();
        // если это кондишенал то парсим
        if (check(TokenType.LOWER_EQUAL) || check(TokenType.BIGGER_EQUAL) || check(TokenType.EQUAL) ||
            check(TokenType.NOT_EQUAL) || check(TokenType.BIGGER_EQUAL) || check(TokenType.LOWER) || check(TokenType.BIGGER) ||
                check(TokenType.IS)) {
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
            // проверка на полное имя класс
            if (check(TokenType.COLON)) {
                consume(TokenType.COLON);
                name = name + ":" + consume(TokenType.ID).value;
            }
            // функция
            if (check(TokenType.BRACKET) && match("(")) {
                // параметры
                ArrayList<Expression> params = new ArrayList<>();
                // аргументы
                consume(TokenType.BRACKET);

                while (!match(")")) {
                    if (!check(TokenType.COMMA)) {
                        params.add(expression());
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
    public AccessExpression parseAccess(boolean isStatement) {
        AccessExpression expr = new AccessExpression(address(), null, isStatement);

        // добавляем акссесс
        expr.add(accessPart());

        while (check(TokenType.DOT)) {
            // консьюм
            consume(TokenType.DOT);
            // добавляем акссесс
            expr.add(accessPart());
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
        ArrayList<String> arguments = new ArrayList<>();
        while (!check(TokenType.BRACKET)) {
            if (!check(TokenType.COMMA)) {
                arguments.add(consume(TokenType.ID).value);
            }
            else {
                consume(TokenType.COMMA);
            }
        }
        // создаём функции
        String fullName = fileName + ":" + name;
        FunctionStatement functionStatement = new FunctionStatement(fullName, name, arguments);
        // скобки
        consume(TokenType.BRACKET);
        // ассигн
        if (check(TokenType.ASSIGN)) {
            consume(TokenType.ASSIGN);
        }
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

    // репит цикл
    private Statement repeat() {
        // паттерн
        // repeat(times) { ... }
        // репит
        consume(TokenType.REPEAT);
        // сколько раз повторить
        consume(TokenType.BRACKET);
        Expression times = expression();
        consume(TokenType.BRACKET);
        // тело
        consume(TokenType.BRACE);
        RepeatStatement repeatStatement = new RepeatStatement(times);
        while (!check(TokenType.BRACE)) {
            repeatStatement.add(statement());
        }
        // фигурная скобка
        consume(TokenType.BRACE);
        // возвращаем стейтмент
        return repeatStatement;
    }

    // парсим стэйтмент
    public Statement statement() {
        // стэйтмент return
        if (check(TokenType.RETURN)) {
            consume(TokenType.RETURN);
            Expression e;
            if (check(TokenType.BRACKET)) {
                consume(TokenType.BRACKET);
                e = expression();
                consume(TokenType.BRACKET);
            }
            else {
                e = expression();
            }
            return new ReturnStatement(e);
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
        // стэйтмент repeat
        if (check(TokenType.REPEAT)) {
            return repeat();
        }
        // стэйтмент ассерт
        if (check(TokenType.ASSERT)) {
            return polarAssert();
        }
        // стэйтмент safe
        if (check(TokenType.SAFE)) {
            return safeHandle();
        }
        // стэйтмент raise
        if (check(TokenType.RAISE)) {
            return raise();
        }
        // стэйтмент juse
        if (check(TokenType.JUSE)) {
            // ДжЮз
            consume(TokenType.JUSE);
            // айдишник
            TextExpression id = (TextExpression) expression();
            // библиотека
            return new JUseStatement(id);
        }
        // стэйтмент new
        if (check(TokenType.NEW)) {
            return parseAccess(true);
        }
        // стэйтмент функции
        if (check(TokenType.FUNC)) {
            return function();
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
            AccessExpression id = parseAccess(true);
            // ==
            if (check(TokenType.ASSIGN)) {
                consume(TokenType.ASSIGN);
                // expression
                Expression value = expression();
                VarAccess last = ((VarAccess) id.getLast());
                id.set(new AssignAccess(id, id.address(), null, last.varName, value, AccessType.SET));
            }
            // +=
            else if (check(TokenType.ASSIGN_ADD)) {
                consume(TokenType.ASSIGN_ADD);
                // expression
                Expression value = expression();
                VarAccess last = ((VarAccess) id.getLast());
                id.set(new AssignAccess(id, id.address(), null, last.varName, value, AccessType.PLUS));
            }
            // -=
            else if (check(TokenType.ASSIGN_SUB)) {
                consume(TokenType.ASSIGN_SUB);
                // expression
                Expression value = expression();
                VarAccess last = ((VarAccess) id.getLast());
                id.set(new AssignAccess(id, id.address(), null, last.varName, value, AccessType.MINUS));
            }
            // *=
            else if (check(TokenType.ASSIGN_MUL)) {
                consume(TokenType.ASSIGN_MUL);
                // expression
                Expression value = expression();
                VarAccess last = ((VarAccess) id.getLast());
                id.set(new AssignAccess(id, id.address(), null, last.varName, value, AccessType.MUL));
            }
            // /=
            else if (check(TokenType.ASSIGN_DIVIDE)) {
                consume(TokenType.ASSIGN_DIVIDE);
                // expression
                Expression value = expression();
                VarAccess last = ((VarAccess) id.getLast());
                id.set(new AssignAccess(id, id.address(), null, last.varName, value, AccessType.DIVIDE));
            }
            // |>
            else if (match("|>")) {
                Expression expr = id;
                while (match("|>")) {
                    consume(TokenType.OPERATOR);
                    Expression _expr = conditional();
                    if (_expr instanceof AccessExpression accessExpression) {
                        expr = new PipeExpression(expr, accessExpression);
                    }
                    else {
                        PolarLogger.exception("Invalid Expression For Pipe: " + _expr.getClass().getName(), _expr.address());
                    }
                }
                if (expr instanceof PipeExpression) {
                    return ((PipeExpression) expr);
                }
                else {
                    PolarLogger.exception("Invalid Expression During Parsing Pipe: "+ expr.getClass().getName(), expr.address());
                }
            }
            else {
                // неизвестный оператор кондишена
                // error("Cannot Use Id Like Statement If Its Not For Assignment Or Call: " + tokenInfo());

                return id;
            }

            return id;
        }
        // стэйтмент юза
        if (check(TokenType.USE)) {
            // юз
            consume(TokenType.USE);
            // айдишник
            TextExpression id = (TextExpression) expression();
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
        Expression expr = expression();
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
        Expression expr = expression();
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
        ArrayList<String> constructor = new ArrayList<>();
        // аргументы конструктора
        while (!check(TokenType.BRACKET)) {
            if (!check(TokenType.COMMA)) {
                constructor.add(consume(TokenType.ID).value);
            } else {
                consume(TokenType.COMMA);
            }
        }
        // полное имя
        String fullName = fileName + ":" + name;
        // стэйтменты
        ClassStatement classStatement = new ClassStatement(fullName, name, constructor);
        // скобка
        consume(TokenType.BRACKET);
        // ассигн
        if (check(TokenType.ASSIGN)) {
            consume(TokenType.ASSIGN);
        }
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
                    Expression expr = expression();
                    // добавляем модульную переменную
                    classStatement.addModuleVariable(idData, expr);
                }
            }
            else {
                PolarLogger.exception("Invalid Statement Creation In Class: " + tokenInfo(), address());
            }
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращение
        return classStatement;
    }

    // логика <<или>>
    private Expression logicalOr() {
        Expression expression = logicalAnd();

        while (check(TokenType.OR)) {
            consume(TokenType.OR);
            expression = new LogicExpression(expression, new Operator("||"), expression());
        }

        return expression;
    }

    // логика <<и>>
    private Expression logicalAnd() {
        Expression expression = conditional();

        while (check(TokenType.AND)) {
            consume(TokenType.AND);
            expression = new LogicExpression(expression, new Operator("&&"), expression());
        }

        return expression;
    }

    // парсинг while
    private Statement whileLoop() {
        // паттерн
        // while (...) { ... }
        consume(TokenType.WHILE);
        // скобка
        consume(TokenType.BRACKET);
        // логика
        Expression expr = logicalOr();
        // брэкет
        consume(TokenType.BRACKET);
        // брэйкс
        consume(TokenType.BRACE);
        // стэйтмент вайл
        WhileStatement statement = new WhileStatement(expr);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг safe
    private Statement safeHandle() {
        // паттерн
        // safe { ... } handle (...) { ... }
        consume(TokenType.SAFE);
        // брэйс
        consume(TokenType.BRACE);
        // стэйтмент трай
        SafeStatement statement = new SafeStatement("");
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.add(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // кэтч
        consume(TokenType.HANDLE);
        // брекет
        consume(TokenType.BRACKET);
        // имя переменной
        String variableName = consume(TokenType.ID).value;
        statement.setVariableName(variableName);
        // брекет
        consume(TokenType.BRACKET);
        // брэйс
        consume(TokenType.BRACE);
        // стэйтменты
        while (!check(TokenType.BRACE)) {
            statement.addHandleStatement(statement());
        }
        // брэйс
        consume(TokenType.BRACE);
        // возвращаем
        return statement;
    }

    // парсинг throw
    private Statement raise() {
        // паттерн
        // raise(...)
        consume(TokenType.RAISE);
        // брэкет
        consume(TokenType.BRACKET);
        // выражение
        Expression expr = expression();
        // брэкет
        consume(TokenType.BRACKET);
        // возвращаем
        return new RaiseStatement(expr);
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
        Expression assignVariableExpr = expression();
        // запятая
        consume(TokenType.COMMA);
        // левый экспрешен
        AccessExpression _l = parseAccess(false);
        // оператор
        Operator _o = condOperator();
        // правый экспрешен
        Expression _r = expression();
        // кондишен
        Expression expr = new ConditionExpression(_l, _o, _r);
        // скобка
        consume(TokenType.BRACKET);
        // брэйкс
        consume(TokenType.BRACE);
        // стэйтмент вайл
        ForStatement statement = new ForStatement(expr, assignVariableName, assignVariableExpr);
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
        AccessExpression lst = parseAccess(false);
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
        Expression e = logicalOr();
        // брэкет
        consume(TokenType.BRACKET);
        // брэйс
        consume(TokenType.BRACE);
        // иф стэйтмент
        IfStatement statement = new IfStatement(e);
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
                Expression _e = logicalOr();
                // брэкет
                consume(TokenType.BRACKET);
                // брэйс
                consume(TokenType.BRACE);
                // иф стэйтмент
                IfStatement _statement = new IfStatement(_e);
                // тело функции
                while (!check(TokenType.BRACE)) {
                    _statement.add(statement());
                }
                // брэйс
                consume(TokenType.BRACE);
                // добавляем в цепочку
                last.setElseCondition(_statement);
                last = _statement;
            }
            else {
                // когда ни одно условие не сработало
                consume(TokenType.ELSE);
                // брэйс
                consume(TokenType.BRACE);
                // кондишены
                Expression _e = new ConditionExpression(new NumberExpression("0"), new Operator("=="), new NumberExpression("0"));
                // стэйтмент
                IfStatement _statement = new IfStatement(_e);
                // стэйтменты
                while (!check(TokenType.BRACE)) {
                    _statement.add(statement());
                }
                // брэйс
                consume(TokenType.BRACE);
                // добавляем в цепочку
                last.setElseCondition(_statement);
                last = _statement;
            }
        }
        // возвращаем
        return statement;
    }

    // парсинг создания объекта
    public Expression objectExpr() {
        // паттерн
        // ... = new %class%()
        consume(TokenType.NEW);
        // айди
        String clazz = consume(TokenType.ID).value;
        // проверяем на полное имя
        if (check(TokenType.COLON)) {
            consume(TokenType.COLON);
            clazz = clazz + ":" + consume(TokenType.ID).value;
        }
        // брэкет
        consume(TokenType.BRACKET);
        // параметры
        ArrayList<Expression> params = new ArrayList<>();
        // конструктор
        while (!match(")")) {
            if (!check(TokenType.COMMA)) {
                params.add(expression());
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
        // имя класса
        consume(TokenType.Q_BRACKET);  // Открывающая скобка
        ArrayList<Expression> expressions = parseList();  // Получаем контейнер
        consume(TokenType.Q_BRACKET);  // Закрывающая скобка
        // рефлексийный экспрешенн
        return new ReflectExpression(className, expressions);
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

    // Парсинг pipe-выражения
    private Expression pipe() {
        Expression expr = logicalOr();

        while (match("|>")) {
            consume(TokenType.OPERATOR);
            Expression _expr = logicalOr();
            if (_expr instanceof AccessExpression accessExpression) {
                expr = new PipeExpression(expr, accessExpression);
            }
            else {
                PolarLogger.exception("Invalid Expression For Pipe: " + _expr.getClass().getName(), _expr.address());
            }
        }

        return expr;
    }

    // тернарное выражение
    private Expression ternary() {
        Expression expr = pipe();
        if (check(TokenType.TERNARY)) {
            consume(TokenType.TERNARY);
            Expression left = expression();
            consume(TokenType.COLON);
            Expression right = expression();
            return new TernaryExpression(expr, left, right);
        }
        return expr;
    }

    // лямбда выражение
    private Expression lambda() {
        if (check(TokenType.LAMBDA)) {
            consume(TokenType.LAMBDA);
            consume(TokenType.BRACKET);
            ArrayList<String> arguments = new ArrayList<>();
            while (!check(TokenType.BRACKET)) {
                if (!check(TokenType.COMMA)) {
                    arguments.add(consume(TokenType.ID).value);
                }
                else {
                    consume(TokenType.COMMA);
                }
            }
            consume(TokenType.BRACKET);
            consume(TokenType.GO);
            consume(TokenType.BRACE);
            LambdaExpression lambdaExpression
                    = new LambdaExpression(arguments);
            while (!check(TokenType.BRACE)) {
                Statement statement = statement();
                lambdaExpression.add(statement);
            }
            consume(TokenType.BRACE);
            return lambdaExpression;
        }
        return ternary();
    }

    // парсинг экспрешенна
    public Expression expression() {
        return lambda();
    }

    // парсинг праймари экспрешенна
    public Expression parsePrimary() {
        // Обработка идентификаторов
        if (check(TokenType.ID) || check(TokenType.NEW)) {
            return parseAccess(false);
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
            Expression expression = expression();  // Рекурсивный вызов parseExpression
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
            Expression lExpr = expression();
            consume(TokenType.COMMA); // Запятая
            Expression rExpr = expression();
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
            Expression key = expression();
            // двоеточие
            consume(TokenType.COLON);
            // значение
            Expression value = expression();
            // помещаем
            container.put(key, value);

            // пока есть запятая -> парсим
            while (check(TokenType.COMMA)) {
                // запятая
                consume(TokenType.COMMA);
                // ключ
                key = expression();
                // двоеточие
                consume(TokenType.COLON);
                // значение
                value = expression();
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
            container.add(expression());

            // пока есть запятая -> парсим
            while (check(TokenType.COMMA)) {
                // запятая
                consume(TokenType.COMMA);
                // экспрешенн
                container.add(expression());
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
        Token token;
        if (current >= tokens.size()) {
            token = tokens.get(current-1);
        } else {
            token = tokens.get(current);
        }
        // трэйс ошибки
        PolarLogger.exception(message, new Address(token.line));
    }

    // токен инфо
    public String tokenInfo() {
        // токен
        if (current >= tokens.size()) {
            return "(out_of_bounds)";
        }
        Token token = tokens.get(current);
        // инфа токена
        return "(" + token.type + ", " + token.value + ", "  + token.line + ")" /*+ " №" + current + " L:"*/;
    }
}
