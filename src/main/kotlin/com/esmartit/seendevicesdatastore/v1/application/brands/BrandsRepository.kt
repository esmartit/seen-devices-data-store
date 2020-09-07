package com.esmartit.seendevicesdatastore.v1.application.brands

import org.springframework.stereotype.Service

data class Brand(val id: String = "", val name: String)

@Service
class BrandsRepository {

    private val brands = listOf(
        "Samsung",
        "Xiaomi",
        "Apple",
        "Huawei",
        "Oppo",
        "LG",
        "Sony Ericsson",
        "Motorola",
        "ZTE"
    ).mapIndexed { index, brand -> Brand(index.toString(), brand) }

    fun findByName(name: String): Brand {
        return brands.firstOrNull {
            it.name.contains(name, true) || name.contains(it.name, true)
        } ?: Brand("", "Others")
    }
}