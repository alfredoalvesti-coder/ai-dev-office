import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface Project {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly http = inject(HttpClient);

  list(page = 0) {
    return this.http.get<Page<Project>>(`/api/projects?page=${page}&size=20`);
  }

  create(input: Pick<Project, 'name' | 'description'>) {
    return this.http.post<Project>('/api/projects', input);
  }
}
