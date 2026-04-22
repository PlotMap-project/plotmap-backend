package com.plotmap.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID


class UserToProjectId(
    val idUser: UUID = UUID.randomUUID(),
    val idProject: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "user_to_projects")
@IdClass(UserToProjectId::class)
class UserToProject(
    @Id
    @Column(name = "id_user")
    val idUser: UUID,

    @Id
    @Column(name = "id_project")
    val idProject: UUID
)
