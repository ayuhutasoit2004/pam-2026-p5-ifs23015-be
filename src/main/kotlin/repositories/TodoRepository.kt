package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.deleteWhere
import java.util.*

class TodoRepository : ITodoRepository {
    override suspend fun getAll(
        userId: String,
        search: String,
        isCompleted: Boolean?,
        urgency: String?,
        page: Int,
        perPage: Int
    ): Pair<List<Todo>, Long> = suspendTransaction {
        var condition: Op<Boolean> = TodoTable.userId eq UUID.fromString(userId)

        if (search.isNotBlank()) {
            val keyword = "%${search.lowercase()}%"
            condition = condition and (TodoTable.title.lowerCase() like keyword)
        }

        if (isCompleted != null) {
            condition = condition and (TodoTable.isDone eq isCompleted)
        }

        if (!urgency.isNullOrBlank()) {
            condition = condition and (TodoTable.urgency eq urgency)
        }

        val total = TodoDAO.find(condition).count()

        val offset = ((page - 1) * perPage).toLong()
        val todos = TodoDAO
            .find(condition)
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .offset(offset)
            .limit(perPage)
            .map(::todoDAOToModel)

        Pair(todos, total)
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find { (TodoTable.id eq UUID.fromString(todoId)) }
            .limit(1)
            .map(::todoDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            urgency = todo.urgency
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }
        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.urgency = newTodo.urgency
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}