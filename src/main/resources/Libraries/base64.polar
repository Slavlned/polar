class Base() {
    mod reflected = reflect 'com.slavlend.Compiler.Libs.Base64' []
    mod func encode(in) {
        return reflected.encode(in)
    }
    mod func decode(in) {
        return reflected.decode(in)
    }
    mod func to_bytes(in) {
        return reflected.bytes(in)
    }
    mod func to_string(in) {
        return reflected.str(in)
    }
}
