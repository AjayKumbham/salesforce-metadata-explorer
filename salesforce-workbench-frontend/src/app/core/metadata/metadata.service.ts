import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface MetadataTypeInfo {
  xmlName: string;
  directoryName: string;
  inFolder: boolean;
  metaFile: boolean;
  suffix: string;
  childXmlNames: string[];
}

export interface MetadataComponentInfo {
  fullName: string;
  type: string;
  fileName: string;
  namespacePrefix: string;
  id: string;
  lastModifiedDate: string;
  createdDate: string;
  lastModifiedByName: string;
  createdByName: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class MetadataService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private get headers(): HttpHeaders {
    const connectionId = this.authService.currentConnectionId();
    return new HttpHeaders({ Authorization: `Bearer ${connectionId}` });
  }

  describeTypes(): Observable<MetadataTypeInfo[]> {
    return this.http.get<MetadataTypeInfo[]>('/api/metadata/types', { headers: this.headers });
  }

  listComponents(type: string, folder: string | undefined, search: string, page: number, size: number): Observable<PaginatedResponse<MetadataComponentInfo>> {
    let url = `/api/metadata/components?type=${encodeURIComponent(type)}&page=${page}&size=${size}`;
    if (folder) url += `&folder=${encodeURIComponent(folder)}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    return this.http.get<PaginatedResponse<MetadataComponentInfo>>(url, { headers: this.headers });
  }

  clearCache(): Observable<void> {
    return this.http.delete<void>('/api/metadata/cache', { headers: this.headers });
  }
}
