import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MetadataService, MetadataTypeInfo, MetadataComponentInfo } from '../../core/metadata/metadata.service';

@Component({
  selector: 'app-metadata',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './metadata.component.html',
  styleUrl: './metadata.component.css'
})
export class MetadataComponent {
  private metadataService = inject(MetadataService);

  types = signal<MetadataTypeInfo[]>([]);
  components = signal<MetadataComponentInfo[]>([]);
  selectedType = signal<MetadataTypeInfo | null>(null);
  
  currentPage = signal(0);
  pageSize = signal(50);
  totalElements = signal(0);
  totalPages = signal(0);
  
  private searchTimeout: any;

  loadingTypes = signal(false);
  loadingComponents = signal(false);
  typesError = signal<string | null>(null);
  componentsError = signal<string | null>(null);
  syncing = signal(false);

  typeSearchQuery = signal('');
  componentSearchQuery = signal('');

  filteredTypes = computed(() => {
    const q = this.typeSearchQuery().toLowerCase();
    return this.types().filter(t => t.xmlName.toLowerCase().includes(q));
  });

  ngOnInit() {
    this.loadTypes();
  }

  loadTypes() {
    this.loadingTypes.set(true);
    this.typesError.set(null);

    this.metadataService.describeTypes().subscribe({
      next: (types) => {
        this.types.set(types);
        this.loadingTypes.set(false);
      },
      error: (err) => {
        this.typesError.set(err?.error?.message || 'Failed to load metadata types');
        this.loadingTypes.set(false);
      }
    });
  }

  selectType(type: MetadataTypeInfo) {
    this.selectedType.set(type);
    this.components.set([]);
    this.componentSearchQuery.set('');
    this.componentsError.set(null);
    this.currentPage.set(0);
    this.loadComponents();
  }

  onSearchChange(query: string) {
    this.componentSearchQuery.set(query);
    this.currentPage.set(0);
    
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.loadComponents();
    }, 400);
  }

  loadComponents() {
    const type = this.selectedType();
    if (!type) return;

    this.loadingComponents.set(true);
    this.componentsError.set(null);

    this.metadataService.listComponents(
      type.xmlName, 
      undefined, 
      this.componentSearchQuery(), 
      this.currentPage(), 
      this.pageSize()
    ).subscribe({
      next: (response) => {
        this.components.set(response.content);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.loadingComponents.set(false);
      },
      error: (err) => {
        this.componentsError.set(err?.error?.message || 'Failed to load components');
        this.loadingComponents.set(false);
      }
    });
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadComponents();
    }
  }

  prevPage() {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadComponents();
    }
  }

  onPageSizeChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement;
    this.pageSize.set(parseInt(selectElement.value, 10));
    this.currentPage.set(0);
    this.loadComponents();
  }

  sync() {
    this.syncing.set(true);
    this.selectedType.set(null);
    this.components.set([]);
    this.metadataService.clearCache().subscribe({
      next: () => {
        this.syncing.set(false);
        this.loadTypes();
      },
      error: (err) => {
        this.syncing.set(false);
        this.typesError.set(err?.error?.message || 'Failed to sync metadata');
      }
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    
    if (isNaN(date.getTime())) return '—';
    
    const pad = (n: number) => n.toString().padStart(2, '0');
    
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const mins = pad(date.getMinutes());
    
    return `${year}-${month}-${day} ${hours}:${mins}`;
  }
}
