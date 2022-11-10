/*
 * MIT License
 *
 * Copyright (c) 2020 Shreyas Patil
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.shreyaspatil.foodium.data.repository

import androidx.annotation.MainThread
import dev.shreyaspatil.foodium.data.local.dao.PostsDao
import dev.shreyaspatil.foodium.data.remote.api.FoodiumService
import dev.shreyaspatil.foodium.model.Post
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import retrofit2.Response
import javax.inject.Inject

interface PostRepository {
    fun getAllPosts(): Flow<Resource<List<Post>>>
    fun getPostById(postId: Int): Flow<Post>
}

/**
 * 用于从远程获取数据并将其存储在数据库中的单一存储库
 * 用于离线功能 这是单一数据源.
 */
@ExperimentalCoroutinesApi
class DefaultPostRepository @Inject constructor(
    private val postsDao: PostsDao,
    private val foodiumService: FoodiumService
) : PostRepository {

    /**
     * 从网络获取帖子并将其存储在数据库中. 最后，来自持久性的数据
     * 获取并发出存储.
     */
    override fun getAllPosts(): Flow<Resource<List<Post>>> {
        return object : NetworkBoundRepository<List<Post>, List<Post>>() {

            override suspend fun saveRemoteData(response: List<Post>) = postsDao.addPosts(response)

            override fun fetchFromLocal(): Flow<List<Post>> = postsDao.getAllPosts()

            override suspend fun fetchFromRemote(): Response<List<Post>> = foodiumService.getPosts()
        }.asFlow()
    }

    /**
     * 检索具有指定[postId]的帖子。
     * @param postId   [Post]的唯一id.
     * @return [Post] 从数据库获取的数据.
     */
    @MainThread
    override fun getPostById(postId: Int): Flow<Post> = postsDao.getPostById(postId).distinctUntilChanged()
}
