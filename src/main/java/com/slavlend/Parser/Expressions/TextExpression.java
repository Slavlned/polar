package com.slavlend.Parser.Expressions;

import com.slavlend.App;
import com.slavlend.Compiler.Compiler;
import com.slavlend.Polar.PolarValue;
import com.slavlend.Parser.Address;
import com.slavlend.VM.Instructions.VmInstrPush;
import lombok.Getter;

/*
Текстовый экспрешенн - возвращает строку
 */
@Getter
public class TextExpression implements Expression {
    // данные
    private final String data;

    // адресс
    private final Address address = App.parser.address();

    @Override
    public PolarValue evaluate() {
        return new PolarValue(data);
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public void compile() {
        Compiler.code.visitInstr(new VmInstrPush(data));
    }

    public TextExpression(String data) {
        this.data = data;
    }
}
