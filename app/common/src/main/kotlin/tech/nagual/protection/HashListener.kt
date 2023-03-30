package tech.nagual.protection

interface HashListener {
    fun receivedHash(hash: String, type: Int)
}
