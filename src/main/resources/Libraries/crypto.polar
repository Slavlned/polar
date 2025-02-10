class Crypto() {
    mod reflected = reflect 'com.slavlend.Compiler.Libs.Crypto' []
    mod func encryptAES(data,key) {
        return Crypto.reflected.encryptAES(data,key)
    }
    mod func decryptAES(data,key) {
        return Crypto.reflected.decryptAES(data,key)
    }
}
