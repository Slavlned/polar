package com.slavlend.VM.Instructions;

import com.slavlend.VM.IceVm;
import com.slavlend.VM.VmFrame;
import com.slavlend.VM.VmFunction;
import com.slavlend.VM.VmInstr;

/*
Помещение переменной в VM
 */
public class VmInstrCall implements VmInstr {
    private final String name;
    private final boolean hasNext;

    public VmInstrCall(String name, boolean hasNext) {
        this.name = name; this.hasNext = hasNext;
    }

    @Override
    public void run(IceVm vm, VmFrame<Object> frame) {
        if (!hasNext) {
            vm.call(name);
        }
    }
}
