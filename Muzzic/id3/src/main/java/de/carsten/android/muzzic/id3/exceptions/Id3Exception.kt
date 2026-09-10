package de.carsten.android.muzzic.id3.exceptions

class Id3Exception : RuntimeException {
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor(msg: String) : super(msg)
    constructor() : super()
}
