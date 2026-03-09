package com.atixedu.haruplanner

object Dummy {
    fun getScheduleData(): String {
        val jsonString = """
            {
                "id": 8,
                "startTime": "2025-05-31T10:00:00.000Z",
                "endTime": "2025-05-31T12:59:00.000Z",
                "title": "물리 문제풀이",
                "description": "필수본, O2문제풀이",
                "logs": [],
                "category": "공부",
                "warningCount": 0,
                "isWarnedAdmin": false,
                "status": 0
            }
            """.trimIndent()
        return jsonString
    }
    fun getScheduleData2(): String {
        val jsonString = """
            {
        "id": 9,
        "startTime": "2025-05-31T15:00:00.000Z",
        "endTime": "2025-05-31T18:59:00.000Z",
        "title": "물리 문제풀이",
        "description": "필수본, O2문제풀이",
        "logs": [],
        "category": "공부",
        "warningCount": 0,
        "isWarnedAdmin": false,
        "status": 0
            }
            """.trimIndent()
        return jsonString
    }
    fun getScheduleData3(): String {
        val jsonString = """
            {
                "id": 10,
                "startTime": "2025-05-31T03:00:00.000Z",
                "endTime": "2025-05-31T05:59:00.000Z",
                "title": "물리 문제풀이",
                "description": "필수본, O2문제풀이",
                "logs": [
                            {"event" : "enter", "timestamp" : "2025-05-31T03:00:00.000Z"}
                        ],
                "category": "공부",
                "warningCount": 0,
                "isWarnedAdmin": false,
                "status": 0
            }       
            """.trimIndent()
        return jsonString
    }


}