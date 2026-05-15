import { copyFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const source = join(root, 'src', 'components.module.css');
const target = join(root, 'dist', 'components.module.css');

mkdirSync(dirname(target), { recursive: true });
copyFileSync(source, target);
