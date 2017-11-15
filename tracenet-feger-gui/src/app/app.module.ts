import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterModule, Routes} from '@angular/router';

import {NavbarModule} from './navbar/navbar.module';
import {StyleModule} from './shared/style.module';

import {AppComponent} from './app.component';

const rootRoutes: Routes = [
    {
        path: 'navbar',
        loadChildren: './navbar/navbar.module#NavbarModule'
    }
];

@NgModule({
    declarations: [
        AppComponent
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        StyleModule.forRoot(),
        NavbarModule.forRoot(),
        RouterModule.forRoot(rootRoutes)
    ],
    providers: [],
    bootstrap: [
        AppComponent
    ]
})
export class AppModule {}
