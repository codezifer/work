import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AngularFontAwesomeModule } from 'angular-font-awesome';

@NgModule({
  imports: [
    CommonModule,
    AngularFontAwesomeModule
  ],
  declarations: [],
})
export class StyleModule {
    static forRoot(): ModuleWithProviders {
      return {
        ngModule: StyleModule,
        providers: []
      }
    }
}
