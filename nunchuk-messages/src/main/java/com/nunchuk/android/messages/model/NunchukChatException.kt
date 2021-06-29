package com.nunchuk.android.messages.model

abstract class NunchukMessageException : Exception()

class SessionLostException : NunchukMessageException()

class RoomCreationException : NunchukMessageException()