package com.example.wechatfriendforcompose.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * 九宫格图片组件
 */
@Composable
fun NineGridImages(
    images: List<String>,
    context: Context,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return
    
    val itemCount = images.size.coerceAtMost(9)
    val columns = when {
        itemCount == 1 -> 1
        itemCount == 4 -> 2
        else -> 3
    }
    val rows = (itemCount + columns - 1) / columns
    
    Column(modifier = modifier) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < itemCount) {
                        val imageName = images[index]
                        val resourceId = getDrawableId(context, imageName)
                        
                        val painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(resourceId)
                                .build()
                        )
                        
                        Image(
                            painter = painter,
                            contentDescription = "图片$index",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 占位空间
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            if (row < rows - 1) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * 获取Drawable资源ID
 */
private fun getDrawableId(context: Context, name: String): Int {
    // 先尝试直接获取
    var resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
    
    // 如果找不到，尝试使用local系列图片
    if (resourceId == 0) {
        val localName = "local${(name.hashCode().and(0x7FFFFFFF) % 11) + 1}"
        resourceId = context.resources.getIdentifier(localName, "drawable", context.packageName)
    }
    
    return resourceId
}


