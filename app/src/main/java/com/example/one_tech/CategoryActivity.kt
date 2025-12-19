package com.example.one_tech

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CategoryActivity : AppCompatActivity() {

    private var isAdminMode = false
    private var categoryName = ""
    private lateinit var productsAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var addProductButton: TextView
    private lateinit var aiAssistantButton: TextView
    private lateinit var bottomNavigation: LinearLayout
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private var currentFilters = FilterData()
    private var allProducts = mutableListOf<Product>()
    private var manufacturers = mutableListOf<String>()
    private var categoryFilterOptions = listOf<CategoryFilterOption>()

    // Для хранения состояния аккордеона (какие открыты)
    private val expandedFilters = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        categoryName = intent.getStringExtra("category_name") ?: "Категория"
        isAdminMode = intent.getBooleanExtra("admin_mode", false)

        initViews()
        setupBackPressedHandler()
        setupCategoryTitle(categoryName)
        setupBackButton()
        setupFilterButton()
        setupRecyclerView()

        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupNormalUserMode()
        }

        loadCategoryFilters()
        loadProducts()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.productsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)
        addProductButton = findViewById(R.id.addProductButton)
        aiAssistantButton = findViewById(R.id.aiAssistantButton)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdminMode) {
                    val intent = Intent(this@CategoryActivity, CatalogActivity::class.java)
                    intent.putExtra("admin_mode", true)
                    startActivity(intent)
                    finish()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupAdminMode() {
        bottomNavigation.visibility = View.GONE
        aiAssistantButton.visibility = View.GONE
        addProductButton.visibility = View.VISIBLE

        addProductButton.setOnClickListener {
            openAddProductActivity()
        }
    }

    private fun setupNormalUserMode() {
        setupClickListeners()
        setupAiAssistantButton()
        addProductButton.visibility = View.GONE
        aiAssistantButton.visibility = View.VISIBLE
    }

    private fun setupCategoryTitle(categoryName: String) {
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = categoryName
    }

    private fun setupBackButton() {
        val backButton = findViewById<TextView>(R.id.backButton)
        backButton.setOnClickListener {
            if (isAdminMode) {
                val intent = Intent(this, CatalogActivity::class.java)
                intent.putExtra("admin_mode", true)
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }
    }

    private fun setupFilterButton() {
        val filterButton = findViewById<TextView>(R.id.filterButton)
        filterButton.setOnClickListener {
            showFiltersDialog()
        }
    }

    private fun loadCategoryFilters() {
        categoryFilterOptions = CategoryFilterHelper.getFiltersForCategory(categoryName)
    }

    private fun showFiltersDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null) as ScrollView
        val dialogContent = dialogView.getChildAt(0) as LinearLayout

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Находим все элементы
        val sortDefault = dialogContent.findViewById<RadioButton>(R.id.sortDefault)
        val sortPriceAsc = dialogContent.findViewById<RadioButton>(R.id.sortPriceAsc)
        val sortPriceDesc = dialogContent.findViewById<RadioButton>(R.id.sortPriceDesc)
        val sortNameAsc = dialogContent.findViewById<RadioButton>(R.id.sortNameAsc)
        val minPriceInput = dialogContent.findViewById<EditText>(R.id.minPriceInput)
        val maxPriceInput = dialogContent.findViewById<EditText>(R.id.maxPriceInput)
        val manufacturerSpinner = dialogContent.findViewById<Spinner>(R.id.manufacturerSpinner)
        val categoryFiltersDivider = dialogContent.findViewById<View>(R.id.categoryFiltersDivider)
        val categoryFiltersTitle = dialogContent.findViewById<TextView>(R.id.categoryFiltersTitle)
        val categoryFiltersContainer = dialogContent.findViewById<LinearLayout>(R.id.categoryFiltersContainer)

        // Устанавливаем текущие значения
        when (currentFilters.sortBy) {
            SortBy.DEFAULT -> sortDefault.isChecked = true
            SortBy.PRICE_ASC -> sortPriceAsc.isChecked = true
            SortBy.PRICE_DESC -> sortPriceDesc.isChecked = true
            SortBy.NAME_ASC -> sortNameAsc.isChecked = true
        }

        minPriceInput.setText(currentFilters.minPrice?.toInt().toString().takeIf { it != "null" } ?: "")
        maxPriceInput.setText(currentFilters.maxPrice?.toInt().toString().takeIf { it != "null" } ?: "")

        setupManufacturerSpinner(manufacturerSpinner)
        setupCategoryFiltersAccordion(categoryFiltersContainer, categoryFiltersDivider, categoryFiltersTitle)

        // Кнопка сброса
        dialogContent.findViewById<Button>(R.id.btnReset).setOnClickListener {
            sortDefault.isChecked = true
            minPriceInput.setText("")
            maxPriceInput.setText("")
            manufacturerSpinner.setSelection(0)
            resetCategoryFilters(categoryFiltersContainer)
            Toast.makeText(this, "Фильтры сброшены", Toast.LENGTH_SHORT).show()
        }

        // Кнопка применения
        dialogContent.findViewById<Button>(R.id.btnApply).setOnClickListener {
            val sortBy = when {
                sortPriceAsc.isChecked -> SortBy.PRICE_ASC
                sortPriceDesc.isChecked -> SortBy.PRICE_DESC
                sortNameAsc.isChecked -> SortBy.NAME_ASC
                else -> SortBy.DEFAULT
            }

            val minPrice = minPriceInput.text.toString().toDoubleOrNull()
            val maxPrice = maxPriceInput.text.toString().toDoubleOrNull()

            val selectedManufacturer = if (manufacturerSpinner.selectedItemPosition > 0) {
                manufacturerSpinner.selectedItem.toString()
            } else {
                null
            }

            val categoryFilters = collectCategoryFilters(categoryFiltersContainer)

            currentFilters = FilterData(
                sortBy = sortBy,
                minPrice = minPrice,
                maxPrice = maxPrice,
                manufacturer = selectedManufacturer,
                categoryFilters = categoryFilters
            )

            applyFilters()
            dialog.dismiss()
            showFilterNotification()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    private fun setupManufacturerSpinner(spinner: Spinner) {
        val manufacturerList = mutableListOf("Все производители").apply {
            addAll(manufacturers)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            manufacturerList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.adapter = adapter

        val currentManufacturer = currentFilters.manufacturer ?: "Все производители"
        val position = manufacturerList.indexOf(currentManufacturer)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    private fun setupCategoryFiltersAccordion(container: LinearLayout, divider: View, title: TextView) {
        if (categoryFilterOptions.isEmpty()) {
            divider.visibility = View.GONE
            title.visibility = View.GONE
            container.visibility = View.GONE
            return
        }

        divider.visibility = View.VISIBLE
        title.visibility = View.VISIBLE
        container.visibility = View.VISIBLE
        container.removeAllViews()

        // Восстанавливаем выбранные фильтры
        val currentCategoryFilters = currentFilters.categoryFilters

        for (filterOption in categoryFilterOptions) {
            // Создаем аккордеон элемент
            val accordionView = LayoutInflater.from(this).inflate(R.layout.filter_accordion_item, null)
            val accordionHeader = accordionView.findViewById<LinearLayout>(R.id.accordionHeader)
            val filterName = accordionView.findViewById<TextView>(R.id.filterName)
            val expandIcon = accordionView.findViewById<ImageView>(R.id.expandIcon)
            val accordionContent = accordionView.findViewById<LinearLayout>(R.id.accordionContent)
            val checkboxesContainer = accordionView.findViewById<LinearLayout>(R.id.checkboxesContainer)
            val selectAllText = accordionView.findViewById<TextView>(R.id.selectAllText)

            filterName.text = filterOption.displayName

            // Создаем чекбоксы для каждого значения
            val checkBoxes = mutableListOf<CheckBox>()
            for (value in filterOption.values) {
                val checkBoxView = LayoutInflater.from(this).inflate(R.layout.checkbox_filter_value, null)
                val checkBox = checkBoxView.findViewById<CheckBox>(R.id.filterCheckbox)
                val valueText = checkBoxView.findViewById<TextView>(R.id.filterValueText)

                valueText.text = value

                // Проверяем, выбран ли этот фильтр
                val isSelected = currentCategoryFilters[filterOption.key]?.contains(value) ?: false
                checkBox.isChecked = isSelected

                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    updateSelectAllButton(checkBoxes, selectAllText)
                }

                checkBoxes.add(checkBox)
                checkboxesContainer.addView(checkBoxView)
            }

            // Кнопка "Выбрать все"
            selectAllText.setOnClickListener {
                val allChecked = checkBoxes.all { it.isChecked }
                checkBoxes.forEach { it.isChecked = !allChecked }
                updateSelectAllButton(checkBoxes, selectAllText)
            }

            updateSelectAllButton(checkBoxes, selectAllText)

            // Обработчик клика на заголовок аккордеона
            accordionHeader.setOnClickListener {
                if (accordionContent.visibility == View.VISIBLE) {
                    // Скрываем
                    accordionContent.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand_more)
                    expandedFilters.remove(filterOption.key)
                } else {
                    // Показываем
                    accordionContent.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_expand_less)
                    expandedFilters.add(filterOption.key)
                }
            }

            // Восстанавливаем состояние аккордеона
            if (filterOption.key in expandedFilters) {
                accordionContent.visibility = View.VISIBLE
                expandIcon.setImageResource(R.drawable.ic_expand_less)
            } else {
                accordionContent.visibility = View.GONE
                expandIcon.setImageResource(R.drawable.ic_expand_more)
            }

            container.addView(accordionView)
        }
    }

    private fun updateSelectAllButton(checkBoxes: List<CheckBox>, selectAllText: TextView) {
        val allChecked = checkBoxes.all { it.isChecked }
        val someChecked = checkBoxes.any { it.isChecked } && !allChecked

        selectAllText.text = when {
            allChecked -> "Снять все"
            someChecked -> "Выбрать все"
            else -> "Выбрать все"
        }
    }

    private fun resetCategoryFilters(container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val accordionView = container.getChildAt(i)
            val accordionContent = accordionView.findViewById<LinearLayout>(R.id.accordionContent)
            val checkboxesContainer = accordionContent.findViewById<LinearLayout>(R.id.checkboxesContainer)

            for (j in 0 until checkboxesContainer.childCount) {
                val checkBoxView = checkboxesContainer.getChildAt(j)
                val checkBox = checkBoxView.findViewById<CheckBox>(R.id.filterCheckbox)
                checkBox.isChecked = false
            }
        }
    }

    private fun collectCategoryFilters(container: LinearLayout): Map<String, List<String>> {
        val filters = mutableMapOf<String, MutableList<String>>()

        for (i in 0 until container.childCount) {
            val accordionView = container.getChildAt(i)
            val filterName = accordionView.findViewById<TextView>(R.id.filterName).text.toString()
            val accordionContent = accordionView.findViewById<LinearLayout>(R.id.accordionContent)

            // Проверяем, есть ли контент (аккордеон может быть закрыт)
            if (accordionContent.visibility == View.VISIBLE) {
                val checkboxesContainer = accordionContent.findViewById<LinearLayout>(R.id.checkboxesContainer)

                // Находим ключ фильтра по displayName
                val filterOption = categoryFilterOptions.find { it.displayName == filterName }
                filterOption?.let { option ->
                    val selectedValues = mutableListOf<String>()

                    for (j in 0 until checkboxesContainer.childCount) {
                        val checkBoxView = checkboxesContainer.getChildAt(j)
                        val checkBox = checkBoxView.findViewById<CheckBox>(R.id.filterCheckbox)
                        val valueText = checkBoxView.findViewById<TextView>(R.id.filterValueText)

                        if (checkBox.isChecked) {
                            selectedValues.add(valueText.text.toString())
                        }
                    }

                    if (selectedValues.isNotEmpty()) {
                        filters[option.key] = selectedValues
                    }
                }
            }
        }

        return filters
    }

    private fun showFilterNotification() {
        val filtersApplied = mutableListOf<String>()

        when (currentFilters.sortBy) {
            SortBy.DEFAULT -> filtersApplied.add("Сортировка: Новые сначала")
            SortBy.PRICE_ASC -> filtersApplied.add("Сортировка: Цена по возрастанию")
            SortBy.PRICE_DESC -> filtersApplied.add("Сортировка: Цена по убыванию")
            SortBy.NAME_ASC -> filtersApplied.add("Сортировка: Название А-Я")
        }

        currentFilters.minPrice?.let {
            filtersApplied.add("Цена от: ${it.toInt()} ₽")
        }

        currentFilters.maxPrice?.let {
            filtersApplied.add("Цена до: ${it.toInt()} ₽")
        }

        currentFilters.manufacturer?.let {
            filtersApplied.add("Производитель: $it")
        }

        currentFilters.categoryFilters.forEach { (key, values) ->
            val filterOption = categoryFilterOptions.find { it.key == key }
            filterOption?.let {
                if (values.size == 1) {
                    filtersApplied.add("${filterOption.displayName}: ${values.first()}")
                } else if (values.size <= 3) {
                    filtersApplied.add("${filterOption.displayName}: ${values.joinToString(", ")}")
                } else {
                    filtersApplied.add("${filterOption.displayName}: ${values.size} выбрано")
                }
            }
        }

        if (filtersApplied.isNotEmpty()) {
            Toast.makeText(this, "Применено:\n${filtersApplied.joinToString("\n")}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        findViewById<LinearLayout>(R.id.navCatalog)?.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navCart)?.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupAiAssistantButton() {
        aiAssistantButton.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductAdapter(
            emptyList(),
            isAdminMode = isAdminMode,
            onItemClick = { product ->
                openProductDetailsActivity(product)
            },
            onAddToCartClick = { product ->
                addToCart(product)
            },
            onEditClick = { product ->
                openEditProductActivity(product)
            },
            onDeleteClick = { product ->
                deleteProduct(product)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
        showLoading(true)

        db.collection("products")
            .whereEqualTo("category", categoryName)
            .get()
            .addOnSuccessListener { documents ->
                allProducts.clear()
                manufacturers.clear()

                for (document in documents) {
                    try {
                        val product = document.toObject(Product::class.java)
                        allProducts.add(product)

                        if (product.manufacturer.isNotBlank() &&
                            !manufacturers.contains(product.manufacturer)) {
                            manufacturers.add(product.manufacturer)
                        }
                    } catch (e: Exception) {
                        println("Ошибка парсинга товара ${document.id}: ${e.message}")
                    }
                }

                manufacturers.sort()
                applyFilters()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Ошибка загрузки товаров: ${exception.message}", Toast.LENGTH_LONG).show()
                showEmptyState(true)
            }
    }

    private fun applyFilters() {
        var filteredProducts = allProducts.toMutableList()

        // Фильтр по цене
        currentFilters.minPrice?.let { minPrice ->
            filteredProducts = filteredProducts.filter { it.price >= minPrice }.toMutableList()
        }

        currentFilters.maxPrice?.let { maxPrice ->
            filteredProducts = filteredProducts.filter { it.price <= maxPrice }.toMutableList()
        }

        // Фильтр по производителю
        currentFilters.manufacturer?.let { manufacturer ->
            filteredProducts = filteredProducts.filter { it.manufacturer == manufacturer }.toMutableList()
        }

        // Фильтры по характеристикам категории
        currentFilters.categoryFilters.forEach { (key, selectedValues) ->
            filteredProducts = filteredProducts.filter { product ->
                val productValue = product.specs[key] ?: ""
                selectedValues.any { selectedValue ->
                    productValue.contains(selectedValue, ignoreCase = true) ||
                            when {
                                key in listOf("cores", "threads", "memorySlots", "sataPorts", "m2Slots", "fansIncluded") -> {
                                    val productNum = productValue.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    val selectedNum = selectedValue.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    productNum != null && selectedNum != null && productNum >= selectedNum
                                }
                                key == "memoryCapacity" || key == "storageCapacity" -> {
                                    val productNum = extractCapacityNumber(productValue)
                                    val selectedNum = extractCapacityNumber(selectedValue)
                                    productNum != null && selectedNum != null && productNum >= selectedNum
                                }
                                key == "power" -> {
                                    val productNum = productValue.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    val selectedNum = selectedValue.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    productNum != null && selectedNum != null && productNum >= selectedNum
                                }
                                key in listOf("gpuClock", "memoryClock", "frequency", "maxFrequency", "fanSpeed") -> {
                                    handleRangeFilter(productValue, selectedValue)
                                }
                                else -> productValue.equals(selectedValue, ignoreCase = true)
                            }
                }
            }.toMutableList()
        }

        // Сортировка
        filteredProducts = when (currentFilters.sortBy) {
            SortBy.DEFAULT -> filteredProducts.sortedByDescending { it.createdAt }.toMutableList()
            SortBy.PRICE_ASC -> filteredProducts.sortedBy { it.price }.toMutableList()
            SortBy.PRICE_DESC -> filteredProducts.sortedByDescending { it.price }.toMutableList()
            SortBy.NAME_ASC -> filteredProducts.sortedBy { it.name.lowercase() }.toMutableList()
        }

        productsAdapter.updateProducts(filteredProducts)

        if (filteredProducts.isEmpty()) {
            showEmptyState(true)
            if (allProducts.isNotEmpty()) {
                emptyStateText.text = "Ничего не найдено\nПопробуйте изменить фильтры"
            } else {
                emptyStateText.text = "В этой категории пока нет товаров"
            }
        } else {
            showEmptyState(false)
        }

        updateProductsCount(filteredProducts.size)
    }

    private fun extractCapacityNumber(value: String): Int? {
        return when {
            value.contains("TB", ignoreCase = true) -> {
                val num = value.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                (num?.times(1000))?.toInt()
            }
            value.contains("GB", ignoreCase = true) -> {
                value.replace(Regex("[^0-9]"), "").toIntOrNull()
            }
            else -> value.replace(Regex("[^0-9]"), "").toIntOrNull()
        }
    }

    private fun handleRangeFilter(productValue: String, selectedValue: String): Boolean {
        val productNum = productValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false

        return when {
            selectedValue.startsWith("до") -> {
                val max = selectedValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false
                productNum <= max
            }
            selectedValue.startsWith("от") -> {
                val min = selectedValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false
                productNum >= min
            }
            selectedValue.contains("-") -> {
                val parts = selectedValue.split("-")
                if (parts.size == 2) {
                    val min = parts[0].replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false
                    val max = parts[1].replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false
                    productNum in min..max
                } else {
                    false
                }
            }
            else -> false
        }
    }

    private fun updateProductsCount(count: Int) {
        val filterButton = findViewById<TextView>(R.id.filterButton)
        val activeFilters = mutableListOf<String>()

        if (currentFilters.minPrice != null || currentFilters.maxPrice != null) {
            activeFilters.add("цена")
        }
        if (currentFilters.manufacturer != null) {
            activeFilters.add("производитель")
        }
        if (currentFilters.categoryFilters.isNotEmpty()) {
            activeFilters.add("${currentFilters.categoryFilters.size} хар-ки")
        }

        if (activeFilters.isNotEmpty()) {
            filterButton.text = "Фильтры (${activeFilters.joinToString(", ")})"
        } else {
            filterButton.text = "Фильтры"
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
        }
    }

    private fun openAddProductActivity() {
        try {
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra("category_name", categoryName)
                putExtra("admin_mode", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка открытия формы добавления: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun openEditProductActivity(product: Product) {
        val intent = Intent(this, EditProductActivity::class.java).apply {
            putExtra("product_id", product.id)
            putExtra("category_name", categoryName)
            putExtra("admin_mode", true)
        }
        startActivity(intent)
    }

    private fun openProductDetailsActivity(product: Product) {
        val intent = Intent(this, ProductDetailsActivity::class.java)
        intent.putExtra("product_id", product.id)
        intent.putExtra("admin_mode", isAdminMode)
        startActivity(intent)
    }

    private fun deleteProduct(product: Product) {
        if (product.id.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID товара не найден", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("products").document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Товар удален!", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToCart(product: Product) {
        if (isAdminMode) {
            Toast.makeText(this, "Администраторы не могут добавлять товары в корзину", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Войдите в аккаунт чтобы добавить в корзину", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("cart")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("productId", product.id)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val cartItem = hashMapOf(
                        "productId" to product.id,
                        "productName" to product.name,
                        "productPrice" to product.price,
                        "productImage" to (product.images.firstOrNull() ?: ""),
                        "quantity" to 1,
                        "category" to product.category,
                        "userId" to currentUser.uid,
                        "addedAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("cart")
                        .add(cartItem)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Товар добавлен в корзину!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Ошибка добавления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val document = documents.documents[0]
                    val currentQuantity = document.getLong("quantity")?.toInt() ?: 1

                    db.collection("cart").document(document.id)
                        .update("quantity", currentQuantity + 1)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Количество товара увеличено!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Ошибка проверки корзины: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}