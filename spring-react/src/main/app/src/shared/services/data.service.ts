import axios, {Axios} from 'axios';
import {environment} from '../environment.ts';
import {TutorialType} from '../types/tutorial.type.ts';

export class DataService {
    private readonly client?: Axios;

    constructor() {
        this.client = axios.create({
            baseURL: environment.API_BASE_URL,
            headers: {
                'Content-Type': 'application/json'
            }
        });
    }

    async getTutorials(): Promise<TutorialType[]> {
        const http = await this.checkClient();
        return http.get('api/tutorials');
    }

    async getTutorial(id: string): Promise<TutorialType> {
        return this.checkClient().then(client => client.get('api/tutorials/' + id))
    }

    async postTutorial(title: string, description: string): Promise<void> {
        const http = await this.checkClient();
        return http.post('api/add/tutorial', {
            title,
            description
        });
    }

    private checkClient(): Promise<Axios> {
        return new Promise((resolve, reject) => {
            if (!this.client) {
                reject(new Error('HTTP client was not initialized!'));
            } else {
                resolve(this.client);
            }
        });
    }
}