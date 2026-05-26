package com.masar.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masar.portal.model.MeResponse
import com.masar.portal.model.RequestItem
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.components.*
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    baseUrl: String,
    driverName: String,
    nid: String,
    token: String,
    data: MeResponse?,
    loading: Boolean,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // قائمة محلية للرسائل عشان نقدر نخفيها بعد القراءة
    var dismissedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ===== رأس الصفحة: ترحيب + صورة المندوب =====
        HeaderCard(baseUrl, driverName, data)

        if (loading && data == null) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandRed)
            }
            return@Column
        }

        if (data?.ok == true) {
            // ===== رسائل المشرف غير المقروءة (مرئية بشكل واضح) =====
            val unread = data.supervisor_messages.filter {
                it.seen_by_driver_at == null && !dismissedIds.contains(it.id)
            }
            if (unread.isNotEmpty()) {
                SupervisorMessagesCard(
                    messages = unread,
                    onDismiss = { msg ->
                        // علّم في الخادم + اخفِ محليًا
                        dismissedIds = dismissedIds + msg.id
                        scope.launch {
                            runCatching {
                                MasarApi(baseUrl).markSupervisorMessageSeen(nid, token, msg.id)
                            }
                        }
                    }
                )
            }

            PerformanceOverviewCard(data)
            SalaryCard(data)
            QuickStatsRow(data)
        }
    }
}

@Composable
private fun HeaderCard(baseUrl: String, driverName: String, data: MeResponse?) {
    val photo = data?.driver?.driver_photo
    // المسار قد يكون نسبيًا (uploads/xxx.jpg) — حوّله لكامل
    val fullPhoto = when {
        photo == null -> null
        photo.startsWith("http") -> photo
        photo.startsWith("/") -> "$baseUrl$photo"
        else -> "$baseUrl/$photo"
    }

    GradientCard(
        gradient = listOf(BrandRed, BrandRedDark),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (fullPhoto != null) {
                    AsyncImage(
                        model = fullPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("مرحبًا", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(
                    driverName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                val cid = data?.driver?.courier_id
                if (!cid.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("معرّف: $cid", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ==== بطاقة رسائل المشرف ====
@Composable
private fun SupervisorMessagesCard(
    messages: List<RequestItem>,
    onDismiss: (RequestItem) -> Unit,
) {
    val typeLabel = mapOf(
        "complaint" to "📢 شكوى من المشرف",
        "accident"  to "🚨 تنبيه حادث",
        "advance"   to "💰 إشعار سلفة",
        "leave"     to "📅 إشعار إجازة",
        "fuel"      to "🔧 صيانة دورية",
        "report"    to "📋 تقرير من المشرف",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BrandRed.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandRed.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "رسائل من المشرف (${messages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandRed,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            messages.forEach { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Ink2,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            typeLabel[msg.type] ?: msg.type,
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandRed,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (!msg.details.isNullOrBlank()) {
                            Text(
                                msg.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TxtPrimary,
                            )
                        }
                        if (msg.amount != null && msg.amount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "المبلغ: ${formatMoney(msg.amount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TxtSoft,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!msg.created_at.isNullOrBlank()) {
                                Text(
                                    msg.created_at.take(16),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TxtDim,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onDismiss(msg) }) {
                                Text("تم القراءة", color = BrandRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceOverviewCard(data: MeResponse) {
    val perf = data.perf
    val sup = data.sup

    MasarCard {
        SectionTitle("نظرة عامة على الأداء")
        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(level = perf?.level_cat ?: "-")
            Column(Modifier.weight(1f)) {
                Text("الفئة الحالية", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                Text(
                    perf?.level_cat ?: "غير مصنّف",
                    style = MaterialTheme.typography.titleMedium,
                    color = TxtPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (perf?.city_rank != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("الترتيب", style = MaterialTheme.typography.labelSmall, color = TxtDim)
                    Text("#${perf.city_rank}", style = MaterialTheme.typography.titleMedium, color = BrandRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // إجمالي الطلبات (من شيت الأداء / المشرف) كرقم بارز
        val totalOrders = data.total_orders ?: data.perf?.volume ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("إجمالي الطلبات", style = MaterialTheme.typography.labelMedium, color = TxtDim)
                Text(
                    "$totalOrders",
                    style = MaterialTheme.typography.displaySmall,
                    color = BrandRed,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (data.on_time_pct != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("في الموعد", style = MaterialTheme.typography.labelMedium, color = TxtDim)
                    val color = when {
                        data.on_time_pct >= 90 -> Green
                        data.on_time_pct >= 80 -> Amber
                        else -> Red
                    }
                    Text(
                        "%.1f%%".format(data.on_time_pct),
                        style = MaterialTheme.typography.headlineSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // التقدم نحو الهدف
        val target = data.salary?.target ?: 600
        val achieved = data.salary?.effective_delivered ?: totalOrders
        ProgressRow(label = "التقدم نحو الهدف ($target طلب)", current = achieved, target = target)

        Spacer(Modifier.height(16.dp))

        // إحصاءات سريعة
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("الإلغاءات", "${sup?.cancellations ?: 0}",
                accent = if ((sup?.cancellations ?: 0) > 0) Red else TxtDim, modifier = Modifier.weight(1f))
            StatCard("أيام العمل", "${sup?.vda_days ?: 0}",
                accent = if ((sup?.vda_days ?: 0) >= 26) Green else Amber, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SalaryCard(data: MeResponse) {
    val s = data.salary ?: return

    // تعديلات للمندوب: إخفاء صافي الراتب والراتب الأساسي
    // عرض فقط: المكافآت (مع تسمية "متوقعة" بدل "أساسية") + الخصومات والغرامات
    // إعادة تسمية "مكافأة أساسية" → "مكافأة متوقعة"
    val renamedBonuses = s.bonuses.map { b ->
        if (b.t.contains("أساسية") || b.t.contains("اساسية") || b.t.equals("مكافأة أساسية", ignoreCase = true)) {
            b.copy(t = "مكافأة متوقعة")
        } else b
    }

    MasarCard {
        SectionTitle("ملخّص الحساب الشهري")
        Spacer(Modifier.height(8.dp))
        Text(
            "هذا ملخّص للمكافآت والخصومات الخاصة بك. الراتب النهائي يُحسب من قِبل المحاسبة.",
            style = MaterialTheme.typography.labelSmall,
            color = TxtDim,
        )
        Spacer(Modifier.height(14.dp))

        // 🟢 الإضافات والمكافآت (مع المكافأة المتوقعة)
        if (renamedBonuses.isNotEmpty()) {
            Text("الإضافات والمكافآت", style = MaterialTheme.typography.labelSmall, color = TxtDim)
            Spacer(Modifier.height(6.dp))
            renamedBonuses.forEach { b ->
                InfoRow(b.t, "+ ${formatMoney(b.a)}", valueColor = Green)
            }
            Spacer(Modifier.height(10.dp))
        }

        // 🔴 الخصومات والغرامات
        if (s.deductions.isNotEmpty()) {
            Divider(color = LineDim)
            Spacer(Modifier.height(10.dp))
            Text("الخصومات والغرامات", style = MaterialTheme.typography.labelSmall, color = TxtDim)
            Spacer(Modifier.height(6.dp))
            s.deductions.forEach { d ->
                InfoRow(d.t, "- ${formatMoney(d.a)}", valueColor = Red)
            }
        }

        // لو ما فيه ولا خصم ولا إضافة (نادر)
        if (renamedBonuses.isEmpty() && s.deductions.isEmpty()) {
            Text(
                "لا توجد إضافات أو خصومات حتى الآن.",
                style = MaterialTheme.typography.bodySmall,
                color = TxtDim,
            )
        }
    }
}

@Composable
private fun QuickStatsRow(data: MeResponse) {
    val orders = data.orders
    MasarCard {
        SectionTitle("ملخّص الطلبات")
        Spacer(Modifier.height(12.dp))
        InfoRow("إجمالي المسلّمة (يومي)", "${orders?.deliv ?: 0}")
        InfoRow("الموكّلة",                "${orders?.acc ?: 0}")
        InfoRow("المرفوضة",               "${orders?.rej ?: 0}",
            valueColor = if ((orders?.rej ?: 0) > 0) Red else TxtPrimary)
        InfoRow("المتأخرة",                "${orders?.late_total ?: 0}",
            valueColor = if ((orders?.late_total ?: 0) > 0) Amber else TxtPrimary)
        InfoRow("أيام عمل (تقرير)", "${orders?.days_count ?: 0} يوم")
        // تاريخ واحد فقط — آخر تاريخ توصيل
        val lastDate = data.last_date ?: orders?.last_d
        if (!lastDate.isNullOrBlank()) {
            InfoRow("آخر تاريخ توصيل", lastDate)
        }
    }
}

internal fun formatMoney(v: Double): String {
    val rounded = (v * 100).toLong() / 100.0
    return "%,.2f ﷼".format(rounded)
}
