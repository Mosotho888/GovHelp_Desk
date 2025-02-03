package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CategoryRepository extends JpaRepository<Category, Long>, PagingAndSortingRepository<Category, Long> {
}
