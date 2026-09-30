package com.guddiz.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.dao.FocDao
import com.guddiz.app.data.local.dao.GiftDao
import com.guddiz.app.data.local.dao.PrPackageDao
import com.guddiz.app.data.local.dao.ProductDao
import com.guddiz.app.data.local.dao.PurchaseDao
import com.guddiz.app.data.local.dao.SaleDao
import com.guddiz.app.data.local.dao.StockMovementDao
import com.guddiz.app.data.local.entity.ExpenseEntity
import com.guddiz.app.data.local.entity.FocEntity
import com.guddiz.app.data.local.entity.GiftEntity
import com.guddiz.app.data.local.entity.GiftItemEntity
import com.guddiz.app.data.local.entity.PrPackageEntity
import com.guddiz.app.data.local.entity.PrPackageItemEntity
import com.guddiz.app.data.local.entity.ProductEntity
import com.guddiz.app.data.local.entity.PurchaseEntity
import com.guddiz.app.data.local.entity.SaleEntity
import com.guddiz.app.data.local.entity.SaleItemEntity
import com.guddiz.app.data.local.entity.StockMovementEntity

@Database(
    entities = [
        ProductEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
        StockMovementEntity::class,
        PrPackageEntity::class,
        PrPackageItemEntity::class,
        GiftEntity::class,
        GiftItemEntity::class,
        FocEntity::class,
        SaleEntity::class,
        SaleItemEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GuddizDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun prPackageDao(): PrPackageDao
    abstract fun giftDao(): GiftDao
    abstract fun focDao(): FocDao
    abstract fun saleDao(): SaleDao

    companion object {
        private const val DB_NAME = "guddiz.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `purchases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` INTEGER NOT NULL, `supplier` TEXT, `purchaseDate` INTEGER NOT NULL, `packagingCost` INTEGER NOT NULL, `shippingCost` INTEGER NOT NULL, `otherCost` INTEGER NOT NULL, `totalCost` INTEGER NOT NULL, `receiptPath` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_productId` ON `purchases` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_purchaseDate` ON `purchases` (`purchaseDate`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `amount` INTEGER NOT NULL, `expenseDate` INTEGER NOT NULL, `vendor` TEXT, `description` TEXT NOT NULL, `relatedProductId` INTEGER, `receiptPath` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_expenseDate` ON `expenses` (`expenseDate`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `type` TEXT NOT NULL, `delta` INTEGER NOT NULL, `referenceType` TEXT, `referenceId` INTEGER, `note` TEXT, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_date` ON `stock_movements` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_referenceType_referenceId` ON `stock_movements` (`referenceType`, `referenceId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pr_packages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipientName` TEXT NOT NULL, `recipientType` TEXT NOT NULL, `contactInfo` TEXT, `campaignName` TEXT, `status` TEXT NOT NULL, `date` INTEGER NOT NULL, `productCost` INTEGER NOT NULL, `packagingCost` INTEGER NOT NULL, `courierCost` INTEGER NOT NULL, `otherCost` INTEGER NOT NULL, `totalCost` INTEGER NOT NULL, `receiptPath` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pr_packages_date` ON `pr_packages` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pr_packages_status` ON `pr_packages` (`status`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `pr_package_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` INTEGER NOT NULL, `lineCost` INTEGER NOT NULL, FOREIGN KEY(`packageId`) REFERENCES `pr_packages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pr_package_items_packageId` ON `pr_package_items` (`packageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pr_package_items_productId` ON `pr_package_items` (`productId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `gifts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipient` TEXT NOT NULL, `occasion` TEXT NOT NULL, `date` INTEGER NOT NULL, `productCost` INTEGER NOT NULL, `packagingCost` INTEGER NOT NULL, `courierCost` INTEGER NOT NULL, `otherCost` INTEGER NOT NULL, `totalCost` INTEGER NOT NULL, `receiptPath` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gifts_date` ON `gifts` (`date`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `gift_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `giftId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` INTEGER NOT NULL, `lineCost` INTEGER NOT NULL, FOREIGN KEY(`giftId`) REFERENCES `gifts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gift_items_giftId` ON `gift_items` (`giftId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gift_items_productId` ON `gift_items` (`productId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `foc_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` INTEGER NOT NULL, `lineCost` INTEGER NOT NULL, `recipient` TEXT NOT NULL, `campaign` TEXT, `date` INTEGER NOT NULL, `receiptPath` TEXT, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_foc_records_date` ON `foc_records` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_foc_records_productId` ON `foc_records` (`productId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE purchases ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pr_packages ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gifts ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foc_records ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN itemSummary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN itemCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sales` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleDate` INTEGER NOT NULL, `channel` TEXT NOT NULL, `customer` TEXT, `totalRevenue` INTEGER NOT NULL, `totalCost` INTEGER NOT NULL, `notes` TEXT, `deletedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_saleDate` ON `sales` (`saleDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_channel` ON `sales` (`channel`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sale_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` INTEGER NOT NULL, `unitCost` INTEGER NOT NULL, `lineRevenue` INTEGER NOT NULL, `lineCost` INTEGER NOT NULL, FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
            }
        }

        @Volatile private var instance: GuddizDatabase? = null

        fun close() {
            instance?.close()
            instance = null
        }

        fun get(context: Context): GuddizDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuddizDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { instance = it }
            }
    }
}
