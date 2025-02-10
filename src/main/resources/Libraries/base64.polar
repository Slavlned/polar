class Base64() {
    mod reflected = reflect 'com.slavlend.Compiler.Libs.PolarBase64' []
    mod func encode(in) {
        return Base64.reflected.encode(in)
    }
    mod func decode(in) {
        return Base64.reflected.decode(in)
    }
    mod func to_bytes(in) {
        return Base64.reflected.bytes(in)
    }
    mod func to_string(in) {
        return Base64.reflected.str(in)
    }
}
