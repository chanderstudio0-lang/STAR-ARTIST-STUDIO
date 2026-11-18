package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.BlogPost
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.util.StudioConstants
import com.example.util.StudioStrings
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Main User & Admin facing Studio Blog Section:
 * Displays studio news, photography tips, gear guides, and behind-the-scenes content.
 */
@Composable
fun BlogScreen(
    language: AppLanguage,
    blogPosts: List<BlogPost>,
    isAdmin: Boolean = false,
    isSubmitting: Boolean = false,
    onPublishPost: (title: String, category: String, content: String, summary: String, imageUrl: String, tags: String, status: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdatePost: (id: String, title: String, category: String, content: String, summary: String, imageUrl: String, tags: String, status: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeletePost: (id: String) -> Unit = {},
    onNavigateToBooking: () -> Unit = {}
) {
    val s = StudioStrings(language)
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPostForDetail by remember { mutableStateOf<BlogPost?>(null) }
    var postToEdit by remember { mutableStateOf<BlogPost?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Filter posts
    val filteredPosts = remember(blogPosts, selectedCategory, searchQuery) {
        blogPosts.filter { post ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "NEWS" -> post.category.equals("Studio News", ignoreCase = true)
                "TIPS" -> post.category.equals("Photography Tips", ignoreCase = true)
                "BTS" -> post.category.equals("Behind The Scenes", ignoreCase = true)
                "GEAR" -> post.category.equals("Gear & Lighting", ignoreCase = true)
                "SPOTLIGHT" -> post.category.equals("Model Spotlight", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                post.title.lowercase().contains(q) ||
                        post.summary.lowercase().contains(q) ||
                        post.content.lowercase().contains(q) ||
                        post.tags.lowercase().contains(q)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("blog_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(
                            BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer)
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Newspaper,
                                    contentDescription = "Studio Journal",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = s.blogTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Star Artist Studio • Talwara Khurd, Sirsa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldLight,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = s.blogSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blog_search_input"),
                            placeholder = {
                                Text(
                                    text = s.searchBlogPlaceholder,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StudioElevatedSurface,
                                unfocusedContainerColor = StudioElevatedSurface,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = StudioOutline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BlogCategoryChip(
                        label = s.filterAll,
                        isSelected = selectedCategory == "ALL",
                        count = blogPosts.size,
                        onClick = { selectedCategory = "ALL" },
                        testTag = "blog_filter_all"
                    )
                    BlogCategoryChip(
                        label = s.filterPhotoTips,
                        isSelected = selectedCategory == "TIPS",
                        accentColor = Color(0xFF10B981),
                        onClick = { selectedCategory = "TIPS" },
                        testTag = "blog_filter_tips"
                    )
                    BlogCategoryChip(
                        label = s.filterStudioNews,
                        isSelected = selectedCategory == "NEWS",
                        accentColor = GoldPrimary,
                        onClick = { selectedCategory = "NEWS" },
                        testTag = "blog_filter_news"
                    )
                    BlogCategoryChip(
                        label = s.filterBts,
                        isSelected = selectedCategory == "BTS",
                        accentColor = Color(0xFF38BDF8),
                        onClick = { selectedCategory = "BTS" },
                        testTag = "blog_filter_bts"
                    )
                    BlogCategoryChip(
                        label = s.filterGear,
                        isSelected = selectedCategory == "GEAR",
                        accentColor = Color(0xFFFBBF24),
                        onClick = { selectedCategory = "GEAR" },
                        testTag = "blog_filter_gear"
                    )
                    BlogCategoryChip(
                        label = s.filterModelSpotlight,
                        isSelected = selectedCategory == "SPOTLIGHT",
                        accentColor = Color(0xFFA855F7),
                        onClick = { selectedCategory = "SPOTLIGHT" },
                        testTag = "blog_filter_spotlight"
                    )
                }
            }

            // Empty State
            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = null,
                                tint = GoldPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = s.noPostsFound,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // Post Cards
                items(filteredPosts, key = { it.id.ifBlank { it.title } }) { post ->
                    BlogPostCard(
                        post = post,
                        language = language,
                        isAdmin = isAdmin,
                        onClick = { selectedPostForDetail = post },
                        onEditClick = { postToEdit = post },
                        onDeleteClick = { onDeletePost(post.id) }
                    )
                }
            }
        }

        // Admin FAB: Publish New Article
        if (isAdmin) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("admin_create_blog_fab"),
                containerColor = GoldPrimary,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Publish Post"
                )
            }
        }
    }

    // Detail Dialog / Bottom View
    selectedPostForDetail?.let { post ->
        BlogPostDetailDialog(
            post = post,
            language = language,
            onDismiss = { selectedPostForDetail = null },
            onNavigateToBooking = {
                selectedPostForDetail = null
                onNavigateToBooking()
            }
        )
    }

    // Create / Publish Dialog
    if (showCreateDialog) {
        BlogPostFormDialog(
            language = language,
            isSubmitting = isSubmitting,
            existingPost = null,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, cat, content, summary, imageUrl, tags, status ->
                onPublishPost(title, cat, content, summary, imageUrl, tags, status)
                showCreateDialog = false
            }
        )
    }

    // Edit Dialog
    postToEdit?.let { post ->
        BlogPostFormDialog(
            language = language,
            isSubmitting = isSubmitting,
            existingPost = post,
            onDismiss = { postToEdit = null },
            onSubmit = { title, cat, content, summary, imageUrl, tags, status ->
                onUpdatePost(post.id, title, cat, content, summary, imageUrl, tags, status)
                postToEdit = null
            }
        )
    }
}

/**
 * Individual Blog Post Card in Obsidian Black & Gold styling.
 */
@Composable
fun BlogPostCard(
    post: BlogPost,
    language: AppLanguage,
    isAdmin: Boolean = false,
    onClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val s = StudioStrings(language)
    val categoryConfig = getCategoryConfig(post.category)

    val formattedDate = remember(post.createdAt) {
        post.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } ?: "Recent"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.dp,
                    categoryConfig.accentColor.copy(alpha = 0.35f)
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("blog_post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional Header Image
            if (post.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, StudioCardSurface.copy(alpha = 0.85f))
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Category Pill & Read Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryConfig.accentColor.copy(alpha = 0.18f))
                                .border(0.5.dp, categoryConfig.accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = post.category.uppercase(Locale.getDefault()),
                                color = categoryConfig.accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (post.status.equals("draft", ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3B1E1E))
                                    .border(0.5.dp, ErrorRed, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DRAFT",
                                    color = ErrorRed,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${post.readTimeMinutes} ${s.minReadLabel}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Excerpt
                if (post.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Row: Author, Date & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.authorName.ifBlank { s.postAuthorLabel },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            color = StudioOutline,
                            fontSize = 10.sp
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }

                    if (isAdmin) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Post",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Post",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = s.readMoreBtn,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full detail reader modal dialog for an article.
 */
@Composable
fun BlogPostDetailDialog(
    post: BlogPost,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onNavigateToBooking: () -> Unit
) {
    val s = StudioStrings(language)
    val context = LocalContext.current
    val categoryConfig = getCategoryConfig(post.category)

    val formattedDate = remember(post.createdAt) {
        post.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        } ?: "Recent"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)), RoundedCornerShape(18.dp)),
            color = StudioCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Close Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (post.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = post.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Category & Read Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryConfig.accentColor.copy(alpha = 0.2f))
                                .border(0.5.dp, categoryConfig.accentColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = post.category.uppercase(Locale.getDefault()),
                                color = categoryConfig.accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${post.readTimeMinutes} ${s.minReadLabel} • $formattedDate",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Author Banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = post.authorName.ifBlank { "Star Artist Studio Official" },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioOutline)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Content Body
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Studio Address & Call to Action Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Experience Star Artist Studio in Person",
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = StudioConstants.ADDRESS,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToBooking,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Book Shoot", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }

                                OutlinedButton(
                                    onClick = { openWhatsApp(context) },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, WhatsAppGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creation and Editing Modal Dialog for Studio Administrators.
 */
@Composable
fun BlogPostFormDialog(
    language: AppLanguage,
    isSubmitting: Boolean,
    existingPost: BlogPost? = null,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, content: String, summary: String, imageUrl: String, tags: String, status: String) -> Unit
) {
    val s = StudioStrings(language)

    var title by remember { mutableStateOf(existingPost?.title ?: "") }
    var category by remember { mutableStateOf(existingPost?.category ?: "Photography Tips") }
    var summary by remember { mutableStateOf(existingPost?.summary ?: "") }
    var content by remember { mutableStateOf(existingPost?.content ?: "") }
    var imageUrl by remember { mutableStateOf(existingPost?.imageUrl ?: "") }
    var tags by remember { mutableStateOf(existingPost?.tags ?: "") }
    var isDraft by remember { mutableStateOf(existingPost?.status.equals("draft", ignoreCase = true)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Photography Tips",
        "Studio News",
        "Behind The Scenes",
        "Gear & Lighting",
        "Model Spotlight"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, GoldPrimary), RoundedCornerShape(16.dp)),
            color = StudioCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (existingPost == null) s.postFormTitle else s.editPostBtn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 16.sp
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                errorMessage?.let { msg ->
                    Text(text = msg, color = ErrorRed, fontSize = 12.sp)
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blog_input_title"),
                    label = { Text(s.postTitleLabel, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    )
                )

                // Category Chips Selector
                Text(
                    text = s.postCategoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldPrimary else StudioElevatedSurface)
                                .border(1.dp, if (isSelected) GoldPrimary else StudioOutline, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Summary Input
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blog_input_summary"),
                    label = { Text(s.postSummaryLabel, fontSize = 12.sp) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    )
                )

                // Main Content Body Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("blog_input_content"),
                    label = { Text(s.postContentLabel, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    )
                )

                // Cover Photo URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.postImageUrlLabel, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    )
                )

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.postTagsLabel, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = StudioOutline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Submit / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (title.trim().length < 3) {
                                errorMessage = "Please enter a valid title (at least 3 characters)"
                            } else if (content.trim().length < 10) {
                                errorMessage = "Content must be at least 10 characters long"
                            } else {
                                errorMessage = null
                                onSubmit(
                                    title.trim(),
                                    category,
                                    content.trim(),
                                    summary.trim(),
                                    imageUrl.trim(),
                                    tags.trim(),
                                    if (isDraft) "draft" else "published"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blog_submit_button"),
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Text(
                                text = if (existingPost == null) "Publish" else "Save Changes",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlogCategoryChip(
    label: String,
    isSelected: Boolean,
    count: Int? = null,
    accentColor: Color = GoldPrimary,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor else StudioCardSurface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) accentColor else StudioOutline
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else TextPrimary
            )
            count?.let {
                Text(
                    text = "($it)",
                    fontSize = 10.sp,
                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextSecondary
                )
            }
        }
    }
}

private data class CategoryConfig(
    val accentColor: Color,
    val icon: ImageVector
)

@Composable
private fun getCategoryConfig(category: String): CategoryConfig {
    val gold = GoldPrimary
    return when (category.lowercase(Locale.getDefault()).trim()) {
        "photography tips", "tips" -> CategoryConfig(Color(0xFF10B981), Icons.Default.Lightbulb)
        "studio news", "news" -> CategoryConfig(gold, Icons.Default.Newspaper)
        "behind the scenes", "bts" -> CategoryConfig(Color(0xFF38BDF8), Icons.Default.Movie)
        "gear & lighting", "gear" -> CategoryConfig(Color(0xFFFBBF24), Icons.Default.HourglassTop)
        "model spotlight", "spotlight" -> CategoryConfig(Color(0xFFA855F7), Icons.Default.Person)
        else -> CategoryConfig(gold, Icons.Default.Article)
    }
}
