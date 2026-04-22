package com.plotmap.backend.repository

import com.plotmap.backend.entity.UserToProject
import com.plotmap.backend.entity.UserToProjectId
import org.springframework.data.jpa.repository.JpaRepository

interface UserToProjectRepository : JpaRepository<UserToProject, UserToProjectId>
