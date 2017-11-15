import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { DashboardModule } from '../dashboard/dashboard.module';
import { LabelsDashboardComponent } from '../dashboard/labels-dashboard/labels-dashboard.component';

import { MenuComponent } from './menu/menu.component';
import { NavbarComponent } from './navbar.component';

import { NavbarService } from './navbar.service';

@NgModule({
  imports: [
    CommonModule,
    DashboardModule
  ],
  declarations: [
    NavbarComponent,
    MenuComponent,
    LabelsDashboardComponent
  ],
  exports: [
    NavbarComponent
  ]
})
export class NavbarModule {
  static forRoot(): ModuleWithProviders {
    return {
      ngModule: NavbarModule,
      providers: [
        NavbarService
      ]
    };
  }
}
