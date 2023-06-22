package com.nunchuk.android.app

import java.io.File

object Main {
   @JvmStatic
   fun main(args: Array<String>) {
       File(System.getProperty("user.dir").orEmpty()).walkTopDown().asSequence()
           .filter {
               it.absolutePath.contains("generated").not()
                       && it.absolutePath.contains("tmp").not()
           }
           .filter { it.name != "Main.kt" }
           .filter { it.name.contains(".java") || it.name.contains(".kt") }.forEach {
               val content = it.readText()
               val removeHeader = if (content.contains("@file:OptIn")) {
                   content.substring(content.indexOf("@file:OptIn"))
               } else {
                   content.substring(content.indexOf("package"))
               }
               val newContent = """
                    /**************************************************************************
                     * This file is part of the Nunchuk software (https://nunchuk.io/)        *
                     * Copyright (C) 2022, 2023 Nunchuk                                       *
                     *                                                                        *
                     * This program is free software; you can redistribute it and/or          *
                     * modify it under the terms of the GNU General Public License            *
                     * as published by the Free Software Foundation; either version 3         *
                     * of the License, or (at your option) any later version.                 *
                     *                                                                        *
                     * This program is distributed in the hope that it will be useful,        *
                     * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
                     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
                     * GNU General Public License for more details.                           *
                     *                                                                        *
                     * You should have received a copy of the GNU General Public License      *
                     * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
                     *                                                                        *
                     **************************************************************************/
                   """.trimIndent() + "\n\n" + removeHeader
               it.writeText(
                   newContent
               )
               println(it.readText())
           }
   }
}
