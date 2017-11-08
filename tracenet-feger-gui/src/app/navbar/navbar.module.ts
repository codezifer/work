import { NgModule, ModuleWithProviders } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';

import { NavbarComponent } from './navbar.component';

import { NavbarService } from './navbar.service';
import { MenuComponent } from './menu/menu.component';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule
  ],
  declarations: [
    NavbarComponent,
    MenuComponent
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
    }
  }
}
