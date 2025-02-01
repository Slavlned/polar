class Tasks() = {
    mod reflected = reflect 'com.slavlend.Compiler.Libs.Threads'
    mod func exec(function, args) = {
        Tasks.reflected.start(function, args)
    }
}
