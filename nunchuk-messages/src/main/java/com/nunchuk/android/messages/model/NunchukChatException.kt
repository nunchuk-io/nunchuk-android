package com.nunchuk.android.messages.model

abstract class NunchukMessageException(override val message: String? = "") : Exception()

class SessionLostException : NunchukMessageException()

class RoomCreationException : NunchukMessageException("Room failed to be created")

class RoomWithTagCreationException : NunchukMessageException()

class RoomNotFoundException(val roomId: String) : NunchukMessageException()